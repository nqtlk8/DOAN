package com.storename.erp.common.aop;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.common.domain.IdempotencyRecord;
import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyRecordRepository idempotencyRepo;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Around("@annotation(IdempotencyProtected)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();
        String idempotencyKey = request.getHeader("Idempotency-Key");

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }

        // Calculate request hash
        java.util.List<Object> serializableArgs = java.util.Arrays.stream(joinPoint.getArgs())
            .filter(arg -> !(arg instanceof jakarta.servlet.http.HttpServletRequest) && !(arg instanceof jakarta.servlet.http.HttpServletResponse))
            .collect(java.util.stream.Collectors.toList());
        String payload = objectMapper.writeValueAsString(serializableArgs);
        String rawToHash = request.getMethod() + ":" + request.getRequestURI() + ":" + payload;
        String requestHash = org.springframework.util.DigestUtils.md5DigestAsHex(rawToHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // 1. Try to acquire the lock by checking/inserting
        boolean isLockAcquired = false;
        try {
            Boolean result = transactionTemplate.execute(status -> {
                Optional<IdempotencyRecord> existing = idempotencyRepo.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    return false; // Already exists, lock not acquired
                }
                
                IdempotencyRecord record = new IdempotencyRecord();
                record.setIdempotencyKey(idempotencyKey);
                record.setRequestHash(requestHash);
                record.setResponseSnapshot("IN_PROGRESS");
                idempotencyRepo.saveAndFlush(record);
                return true; // Lock acquired
            });
            isLockAcquired = Boolean.TRUE.equals(result);
        } catch (DataIntegrityViolationException e) {
            isLockAcquired = false; // Another thread inserted it right before we did
        }

        if (!isLockAcquired) {
            // Already processing or processed. Read the state.
            IdempotencyRecord existing = transactionTemplate.execute(status -> 
                idempotencyRepo.findByIdempotencyKey(idempotencyKey).orElse(null)
            );
            
            if (existing != null) {
                if (!requestHash.equals(existing.getRequestHash())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key reused for different request");
                }
                if ("IN_PROGRESS".equals(existing.getResponseSnapshot())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is currently processing. Please try again or wait.");
                }
                log.info("Idempotency key '{}' already processed, returning cached response", idempotencyKey);
                return deserializeResponse(existing.getResponseSnapshot(), joinPoint);
            }
            // Edge case: it was deleted while we were reading?
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrent request conflict. Please try again.");
        }

        // 2. We acquired the lock. Proceed with business logic.
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
            log.error("Failed to save response snapshot for idempotency key", ex);
            // Result was successful, but we failed to cache it. We still return the result to client.
        }

        return result;
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
