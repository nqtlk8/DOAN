package com.storename.erp.inventory.infrastructure;

import com.storename.erp.inventory.domain.CostLayer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.UUID;

public interface CostLayerRepository extends JpaRepository<CostLayer, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CostLayer c WHERE c.productId=:pid AND c.branchId=:bid " +
           "AND c.remainingQty > 0 ORDER BY c.createdAt ASC, c.id ASC")
    List<CostLayer> findAvailableForFifoWithLock(@Param("pid") Long productId, @Param("bid") Long branchId);

    /** Lớp giá vốn tạo gần nhất (kể cả đã dùng hết) — giá vốn dự phòng cho hàng trả không xác định được nguồn. */
    java.util.Optional<CostLayer> findFirstByProductIdAndBranchIdOrderByCreatedAtDesc(Long productId, Long branchId);
}
