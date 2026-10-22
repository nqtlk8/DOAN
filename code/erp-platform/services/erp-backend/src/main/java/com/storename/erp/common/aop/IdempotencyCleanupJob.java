package com.storename.erp.common.aop;

import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Dọn bản ghi {@code idempotency_record} cũ. Bảng nằm cục bộ ở mỗi instance (không replicate) nên job chạy
 * ở mọi instance.
 *
 * <p>Thời hạn giữ ({@code idempotency.retention}, mặc định 1 ngày) phải dài hơn thời gian một phiếu có thể
 * còn mở chờ người dùng bấm Lưu lại. Sau thời hạn này, chứng từ vẫn không bị tạo trùng nhờ cột
 * {@code request_key} (xem {@link IdempotencyContext}).</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "idempotency.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class IdempotencyCleanupJob {

    private final IdempotencyRecordRepository repository;
    private final Duration retention;

    public IdempotencyCleanupJob(IdempotencyRecordRepository repository,
                                 @Value("${idempotency.retention:P1D}") Duration retention) {
        this.repository = repository;
        this.retention = retention;
    }

    @Scheduled(cron = "${idempotency.cleanup.cron:0 30 3 * * *}")
    @Transactional
    public int purgeExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minus(retention);
        int deleted = repository.deleteCreatedBefore(cutoff);
        if (deleted > 0) {
            log.info("Idempotency cleanup: deleted {} record(s) created before {}", deleted, cutoff);
        }
        return deleted;
    }
}
