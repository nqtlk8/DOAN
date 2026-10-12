package com.storename.erp.common.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.common.domain.IdempotencyRecord;
import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

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

    private static final String KEY = "key-123";
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
        request.addHeader("Idempotency-Key", KEY);
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
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("currently processing");
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("Key đã dùng cho nội dung khác: không thu hồi (hash khác) -> 409 reused")
    void differentPayload_isNotReclaimed() throws Throwable {
        when(repo.findByIdempotencyKey(KEY)).thenReturn(Optional.of(inProgress("another-hash")));
        when(repo.deleteStaleInProgress(eq(KEY), anyString(), any())).thenReturn(0);

        assertThatThrownBy(() -> aspect.checkIdempotency(joinPoint))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("reused for different request");
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

    /** Phương thức mẫu để aspect đọc kiểu trả về khi deserialize snapshot. */
    public static class Sample {
        public String handle(String body) {
            return body;
        }
    }
}
