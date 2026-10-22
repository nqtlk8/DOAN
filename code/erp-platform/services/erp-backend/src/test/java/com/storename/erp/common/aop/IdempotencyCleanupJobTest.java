package com.storename.erp.common.aop;

import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IdempotencyCleanupJobTest {

    @Test
    @DisplayName("Xóa bản ghi cũ hơn thời hạn giữ (mặc định 1 ngày)")
    void purgeExpired_deletesRecordsOlderThanRetention() {
        IdempotencyRecordRepository repo = mock(IdempotencyRecordRepository.class);
        when(repo.deleteCreatedBefore(any())).thenReturn(3);
        IdempotencyCleanupJob job = new IdempotencyCleanupJob(repo, Duration.ofDays(1));

        LocalDateTime before = LocalDateTime.now();
        int deleted = job.purgeExpired();

        assertThat(deleted).isEqualTo(3);
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repo).deleteCreatedBefore(cutoff.capture());
        assertThat(cutoff.getValue())
                .isAfterOrEqualTo(before.minusDays(1).minusSeconds(5))
                .isBeforeOrEqualTo(before.minusDays(1).plusSeconds(5));
    }
}
