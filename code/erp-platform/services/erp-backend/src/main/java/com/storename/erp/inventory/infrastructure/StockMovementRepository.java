package com.storename.erp.inventory.infrastructure;

import com.storename.erp.inventory.domain.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByProductIdAndBranchIdOrderByCreatedAtAsc(Long productId, Long branchId);

    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m WHERE m.productId=:pid AND m.branchId=:bid")
    BigDecimal sumQuantity(@Param("pid") Long productId, @Param("bid") Long branchId);
}
