package com.storename.erp.common.infrastructure;

import com.storename.erp.common.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Xóa bản ghi còn IN_PROGRESS, cùng nội dung request, đã tạo trước {@code cutoff} (tiến trình xử lý đã chết giữa chừng).
     * Điều kiện nằm trong câu DELETE nên chỉ một request thu hồi được, và bản ghi đã xong thì không bị xóa.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            "DELETE FROM IdempotencyRecord r WHERE r.idempotencyKey = :key "
                    + "AND r.requestHash = :requestHash "
                    + "AND r.responseSnapshot = 'IN_PROGRESS' AND r.createdAt < :cutoff")
    int deleteStaleInProgress(@org.springframework.data.repository.query.Param("key") String key,
                              @org.springframework.data.repository.query.Param("requestHash") String requestHash,
                              @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff);
}
