package com.storename.erp.common.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.common.domain.IdempotencyRecord;
import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Bản ghi IN_PROGRESS quá hạn (tiến trình chết giữa chừng) được thu hồi để request gửi lại chạy được;
 * bản ghi còn trong hạn vẫn trả 409 như trước.
 */
class IdempotencyAspectStaleTest {

    private static final String HEADER_KEY = "key-123";
    /** Key lưu trong DB = phạm vi người dùng + ":" + header (không đăng nhập -> "anonymous"). */
    private static final String KEY = "anonymous:" + HEADER_KEY;
    private static final String URI = "/api/v1/sales-invoices";
    private static final String ARG = "payload";

    private final IdempotencyRecordRepository repo = mock(IdempotencyRecordRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionTemplate tx = mock(TransactionTemplate.class);
    private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    private final IdempotencyAspect aspect = new IdempotencyAspect(repo, objectMapper, tx, Duration.ofMinutes(10));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", URI);
        request.addHeader("Idempotency-Key", HEADER_KEY);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(null));
        when(joinPoint.getArgs()).thenReturn(new Object[]{ARG});
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(Sample.class.getMethod("handle", String.class));
        when(joinPoint.getSignature()).thenReturn(signature);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    /** Cùng công thức với IdempotencyAspect: md5(method:URI:json(args)). */
    private String expectedHash() throws Exception {
        String payload = objectMapper.writeValueAsString(List.of(ARG));
        return DigestUtils.md5DigestAsHex(("POST:" + URI + ":" + payload).getBytes(StandardCharsets.UTF_8));
    }

    private IdempotencyRecord inProgress(String hash) {
        IdempotencyRecord r = new IdempotencyRecord();
        r.setIdempotencyKey(KEY);
        r.setRequestHash(hash);
        r.setResponseSnapshot("IN_PROGRESS");
        return r;
    }

    @Test
    @DisplayName("IN_PROGRESS quá hạn: xóa có điều kiện, giành khóa lại và chạy nghiệp vụ")
    void staleInProgress_isReclaimedAndExecuted() throws Throwable {
        IdempotencyRecord fresh = inProgress(expectedHash());
        when(repo.findByIdempotencyKey(KEY))
                .thenReturn(Optional.of(inProgress(expectedHash()))) // lần giành khóa 1: còn bản ghi cũ
                .thenReturn(Optional.empty())                        // lần giành khóa 2: đã thu hồi
                .thenReturn(Optional.of(fresh));                     // lưu snapshot kết quả
        when(repo.deleteStaleInProgress(eq(KEY), anyString(), any())).thenReturn(1);
        when(joinPoint.proceed()).thenReturn("OK");

        LocalDateTime before = LocalDateTime.now();
        Object result = aspect.checkIdempotency(joinPoint);

        assertThat(result).isEqualTo("OK");
        verify(joinPoint).proceed();
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repo).deleteStaleInProgress(eq(KEY), eq(expectedHash()), cutoff.capture());
        assertThat(cutoff.getValue()).isBeforeOrEqualTo(before.minusMinutes(10).plusSeconds(5))
                .isAfterOrEqualTo(before.minusMinutes(10).minusSeconds(5));
        assertThat(fresh.getResponseSnapshot()).isEqualTo("\"OK\"");
    }

    @Test
    @DisplayName("IN_PROGRESS còn trong hạn: không thu hồi được -> 409, không chạy nghiệp vụ")
    void freshInProgress_isStillConflict() throws Throwable {
        when(repo.findByIdempotencyKey(KEY)).thenReturn(Optional.of(inProgress(expectedHash())));
        when(repo.deleteStaleInProgress(eq(KEY), anyString(), any())).thenReturn(0);

        assertThatThrownBy(() -> aspect.checkIdempotency(joinPoint))
                .isInstanceOf(BusinessConflictException.class)
                .satisfies(e -> assertThat(((BusinessConflictException) e).getCode()).isEqualTo(IdempotencyAspect.IN_PROGRESS));
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Key đã dùng cho nội dung khác: không thu hồi (hash khác) -> 409 reused")
    void differentPayload_isNotReclaimed() throws Throwable {
        when(repo.findByIdempotencyKey(KEY)).thenReturn(Optional.of(inProgress("another-hash")));
        when(repo.deleteStaleInProgress(eq(KEY), anyString(), any())).thenReturn(0);

        assertThatThrownBy(() -> aspect.checkIdempotency(joinPoint))
                .isInstanceOf(BusinessConflictException.class)
                .satisfies(e -> assertThat(((BusinessConflictException) e).getCode()).isEqualTo(IdempotencyAspect.KEY_REUSED));
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Đã xử lý xong: trả lại kết quả đã lưu, không chạy lại nghiệp vụ")
    void completed_returnsCachedResponse() throws Throwable {
        IdempotencyRecord done = inProgress(expectedHash());
        done.setResponseSnapshot("\"CACHED\"");
        when(repo.findByIdempotencyKey(KEY)).thenReturn(Optional.of(done));
        when(repo.deleteStaleInProgress(eq(KEY), anyString(), any())).thenReturn(0);

        Object result = aspect.checkIdempotency(joinPoint);

        assertThat(result).isEqualTo("CACHED");
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Key lưu theo người dùng: người khác gửi cùng header không đụng bản ghi của người trước")
    void key_isScopedByAuthenticatedUser() throws Throwable {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user-A", null, List.of()));
        when(repo.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(joinPoint.proceed()).thenReturn("OK");

        aspect.checkIdempotency(joinPoint);

        ArgumentCaptor<IdempotencyRecord> saved = ArgumentCaptor.forClass(IdempotencyRecord.class);
        verify(repo).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getIdempotencyKey()).isEqualTo("user-A:" + HEADER_KEY);
        assertThat(saved.getValue().getResponseSnapshot()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("Trong lúc chạy nghiệp vụ, IdempotencyContext trả về key đã gắn phạm vi (để ghi request_key)")
    void requestKey_isBoundDuringBusinessCall() throws Throwable {
        when(repo.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        String[] seen = new String[1];
        when(joinPoint.proceed()).thenAnswer(inv -> {
            seen[0] = IdempotencyContext.currentRequestKey();
            return "OK";
        });

        aspect.checkIdempotency(joinPoint);

        assertThat(seen[0]).isEqualTo(KEY);
    }

    @Test
    @DisplayName("Không gửi header: chạy nghiệp vụ bình thường, không ghi bản ghi, không có request_key")
    void noHeader_proceedsWithoutRecord() throws Throwable {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest("POST", URI)));
        String[] seen = {"unset"};
        when(joinPoint.proceed()).thenAnswer(inv -> {
            seen[0] = IdempotencyContext.currentRequestKey();
            return "OK";
        });

        assertThat(aspect.checkIdempotency(joinPoint)).isEqualTo("OK");
        assertThat(seen[0]).isNull();
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("Header quá dài -> 400, không chạy nghiệp vụ")
    void tooLongKey_isRejected() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", URI);
        request.addHeader("Idempotency-Key", "x".repeat(IdempotencyAspect.MAX_KEY_LENGTH + 1));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertThatThrownBy(() -> aspect.checkIdempotency(joinPoint)).isInstanceOf(IllegalArgumentException.class);
        verify(joinPoint, never()).proceed();
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("Nghiệp vụ lỗi: xóa bản ghi IN_PROGRESS để gửi lại cùng key chạy được")
    void businessFailure_releasesKey() throws Throwable {
        IdempotencyRecord inserted = inProgress(expectedHash());
        when(repo.findByIdempotencyKey(KEY)).thenReturn(Optional.empty()).thenReturn(Optional.of(inserted));
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("thiếu hàng"));

        assertThatThrownBy(() -> aspect.checkIdempotency(joinPoint)).isInstanceOf(IllegalStateException.class);
        verify(repo).delete(inserted);
    }

    /** Phương thức mẫu để aspect đọc kiểu trả về khi deserialize snapshot. */
    public static class Sample {
        public String handle(String body) {
            return body;
        }
    }
}
