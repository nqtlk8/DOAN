package com.storename.erp.order.infrastructure;

import com.storename.erp.order.domain.GoodsReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoodsReturnRepository extends JpaRepository<GoodsReturn, UUID> {
    Optional<GoodsReturn> findByReturnCode(String returnCode);
    boolean existsByReturnCode(String returnCode);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(grl.quantity), 0) FROM GoodsReturnLine grl " +
        "WHERE grl.goodsReturn.invoiceId = :invoiceId " +
        "AND grl.goodsReturn.status = 'CONFIRMED' " +
        "AND grl.productId = :productId"
    )
    java.math.BigDecimal getTotalReturnedQuantity(@org.springframework.data.repository.query.Param("invoiceId") UUID invoiceId, 
                                                  @org.springframework.data.repository.query.Param("productId") Long productId);
    java.util.List<GoodsReturn> findByBranchId(Long branchId);
}
