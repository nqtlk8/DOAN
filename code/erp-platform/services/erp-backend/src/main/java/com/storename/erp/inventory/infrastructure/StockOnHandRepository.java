package com.storename.erp.inventory.infrastructure;

import com.storename.erp.inventory.domain.StockOnHand;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockOnHandRepository extends JpaRepository<StockOnHand, UUID> {
    Optional<StockOnHand> findByProductIdAndBranchId(Long productId, Long branchId);
    java.util.List<StockOnHand> findByBranchId(Long branchId);

    /** Khóa ghi dòng tồn khi xuất bán: hai đơn đồng thời không thể cùng vượt qua bước kiểm tra tồn. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockOnHand s WHERE s.productId = :pid AND s.branchId = :bid")
    Optional<StockOnHand> findForUpdate(@Param("pid") Long productId, @Param("bid") Long branchId);
}
