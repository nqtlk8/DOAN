package com.storename.erp.common.aop;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.common.domain.IdempotencyRecord;
import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Chống xử lý trùng các request ghi chứng từ có header {@code Idempotency-Key}.
 *
 * <p>Frontend tạo một key cho mỗi lần lập phiếu (khi bấm "Thêm mới") và gửi lại đúng key đó ở mọi lần bấm
 * Lưu, kể cả gửi lại sau khi mạng lỗi. Aspect ghi key vào {@code idempotency_record} trước khi chạy
 * nghiệp vụ:</p>
 * <ul>
 *   <li>key mới: chạy nghiệp vụ, lưu kết quả; nghiệp vụ lỗi thì xóa key để được gửi lại;</li>
 *   <li>key đã xong, cùng nội dung: trả lại đúng kết quả cũ, không chạy lại;</li>
 *   <li>key đang chạy: HTTP 409 {@value #IN_PROGRESS};</li>
 *   <li>key đã dùng cho nội dung khác: HTTP 409 {@value #KEY_REUSED}.</li>
 * </ul>
 *
 * <p>Key được lưu theo phạm vi người dùng ({@code <subject>:<key>}): người khác gửi cùng key không nhận được
 * kết quả đã lưu của người trước. Điều này cần thiết vì aspect chạy trước kiểm tra {@code @PreAuthorize}.</p>
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdempotencyAspect {

    public static final String HEADER = "Idempotency-Key";
    /** Mã lỗi (HTTP 409): request đầu tiên với key này vẫn đang được xử lý. Gửi lại cùng key sau ít giây. */
    public static final String IN_PROGRESS = "IDEMPOTENCY_IN_PROGRESS";
    /** Mã lỗi (HTTP 409): key đã được dùng cho một request có nội dung khác. */
    public static final String KEY_REUSED = "IDEMPOTENCY_KEY_REUSED";
    /** Độ dài tối đa của header; cột idempotency_key (255) còn chứa thêm phạm vi người dùng. */
    static final int MAX_KEY_LENGTH = 200;
    static final String IN_PROGRESS_MARKER = "IN_PROGRESS";
    private static final String ANONYMOUS_SCOPE = "anonymous";

    private final IdempotencyRecordRepository idempotencyRepo;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    /**
     * Thời gian tối đa một request được giữ trạng thái IN_PROGRESS. Quá thời hạn này coi như tiến trình
     * xử lý đã dừng giữa chừng (mất điện, khởi động lại) và giao dịch nghiệp vụ đã rollback, nên request
     * gửi lại với cùng key được phép chạy lại thay vì nhận 409 vĩnh viễn.
     */
    private final java.time.Duration inProgressTimeout;

    public IdempotencyAspect(IdempotencyRecordRepository idempotencyRepo, ObjectMapper objectMapper,
                             TransactionTemplate transactionTemplate,
                             @Value("${idempotency.in-progress-timeout:PT10M}") java.time.Duration inProgressTimeout) {
        this.idempotencyRepo = idempotencyRepo;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.inProgressTimeout = inProgressTimeout;
    }

    @Around("@annotation(IdempotencyProtected)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String headerKey = request.getHeader(HEADER);

        if (headerKey == null || headerKey.isBlank()) {
            return joinPoint.proceed();
        }
        headerKey = headerKey.trim();
        if (headerKey.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Idempotency-Key quá dài (tối đa " + MAX_KEY_LENGTH + " ký tự)");
        }
        final String idempotencyKey = currentScope() + ":" + headerKey;

        // Calculate request hash
        java.util.List<Object> serializableArgs = java.util.Arrays.stream(joinPoint.getArgs())
            .filter(arg -> !(arg instanceof jakarta.servlet.http.HttpServletRequest) && !(arg instanceof jakarta.servlet.http.HttpServletResponse))
            .collect(java.util.stream.Collectors.toList());
        String payload = objectMapper.writeValueAsString(serializableArgs);
        String rawToHash = request.getMethod() + ":" + request.getRequestURI() + ":" + payload;
        String requestHash = org.springframework.util.DigestUtils.md5DigestAsHex(rawToHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // 1. Try to acquire the lock by checking/inserting
        boolean isLockAcquired = tryAcquire(idempotencyKey, requestHash);

        // Bản ghi IN_PROGRESS quá hạn: thu hồi (xóa có điều kiện) rồi thử giành khóa lại đúng một lần.
        if (!isLockAcquired && reclaimStaleInProgress(idempotencyKey, requestHash)) {
            log.warn("Idempotency key '{}' was IN_PROGRESS longer than {}; reclaimed for a new attempt",
                    idempotencyKey, inProgressTimeout);
            isLockAcquired = tryAcquire(idempotencyKey, requestHash);
        }

        if (!isLockAcquired) {
            // Already processing or processed. Read the state.
            IdempotencyRecord existing = transactionTemplate.execute(status ->
                idempotencyRepo.findByIdempotencyKey(idempotencyKey).orElse(null)
            );

            if (existing != null) {
                if (!requestHash.equals(existing.getRequestHash())) {
                    throw new BusinessConflictException(KEY_REUSED,
                            "Phiếu này đã được gửi trước đó với nội dung khác. Vui lòng kiểm tra danh sách phiếu.");
                }
                if (IN_PROGRESS_MARKER.equals(existing.getResponseSnapshot())) {
                    throw new BusinessConflictException(IN_PROGRESS,
                            "Yêu cầu trước đó vẫn đang được xử lý. Vui lòng chờ trong giây lát.");
                }
                log.info("Idempotency key '{}' already processed, returning cached response", idempotencyKey);
                return deserializeResponse(existing.getResponseSnapshot(), joinPoint);
            }
            // Bản ghi vừa bị xóa (request đầu lỗi nghiệp vụ) giữa hai lần đọc: client gửi lại là chạy được.
            throw new BusinessConflictException(IN_PROGRESS,
                    "Yêu cầu trước đó vừa kết thúc. Vui lòng gửi lại.");
        }

        // 2. We acquired the lock. Proceed with business logic.
        IdempotencyContext.bind(attributes, idempotencyKey);
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // Business logic failed! We must remove the IN_PROGRESS record so the client can retry.
            try {
                transactionTemplate.execute(status -> {
                    Optional<IdempotencyRecord> existing = idempotencyRepo.findByIdempotencyKey(idempotencyKey);
                    existing.ifPresent(idempotencyRepo::delete);
                    return null;
                });
            } catch (Exception ex) {
                log.error("Failed to clean up idempotency lock after business exception", ex);
            }
            throw e; // rethrow the business exception
        }

        // 3. Business logic succeeded. Update the snapshot.
        try {
            String snapshot = objectMapper.writeValueAsString(result);
            transactionTemplate.execute(status -> {
                Optional<IdempotencyRecord> existing = idempotencyRepo.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    IdempotencyRecord record = existing.get();
                    record.setResponseSnapshot(snapshot);
                    idempotencyRepo.save(record);
                }
                return null;
            });
        } catch (Exception ex) {
            // Kết quả vẫn trả cho client. Nếu sau này gửi lại cùng key, bản ghi IN_PROGRESS quá hạn được thu
            // hồi và chứng từ có request_key sẽ được trả lại thay vì tạo mới (xem IdempotencyContext).
            log.error("Failed to save response snapshot for idempotency key", ex);
        }

        return result;
    }

    /** Phạm vi của key: subject của người dùng đã xác thực (publicId), hoặc "anonymous". */
    private static String currentScope() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return ANONYMOUS_SCOPE;
        }
        return auth.getName();
    }

    private boolean tryAcquire(String idempotencyKey, String requestHash) {
        try {
            Boolean result = transactionTemplate.execute(status -> {
                Optional<IdempotencyRecord> existing = idempotencyRepo.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    return false; // Already exists, lock not acquired
                }

                IdempotencyRecord record = new IdempotencyRecord();
                record.setIdempotencyKey(idempotencyKey);
                record.setRequestHash(requestHash);
                record.setResponseSnapshot(IN_PROGRESS_MARKER);
                idempotencyRepo.saveAndFlush(record);
                return true; // Lock acquired
            });
            return Boolean.TRUE.equals(result);
        } catch (DataIntegrityViolationException e) {
            return false; // Another thread inserted it right before we did
        }
    }

    private boolean reclaimStaleInProgress(String idempotencyKey, String requestHash) {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minus(inProgressTimeout);
        Integer deleted = transactionTemplate.execute(status ->
                idempotencyRepo.deleteStaleInProgress(idempotencyKey, requestHash, cutoff));
        return deleted != null && deleted > 0;
    }

    private Object deserializeResponse(String snapshot, ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            JavaType javaType = objectMapper.getTypeFactory().constructType(signature.getMethod().getGenericReturnType());
            return objectMapper.readValue(snapshot, javaType);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to deserialize idempotency response", ex);
        }
    }
}
