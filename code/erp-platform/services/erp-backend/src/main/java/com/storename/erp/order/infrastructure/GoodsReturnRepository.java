package com.storename.erp.order.infrastructure;

import com.storename.erp.order.domain.GoodsReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GoodsReturnRepository extends JpaRepository<GoodsReturn, UUID> {
    boolean existsByReturnCode(String returnCode);

    /** Chứng từ đã tạo với khóa lập phiếu này (gửi lại cùng Idempotency-Key -> trả lại chứng từ cũ). */
    java.util.Optional<com.storename.erp.order.domain.GoodsReturn> findByRequestKey(String requestKey);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(grl.quantity), 0) FROM GoodsReturnLine grl " +
        "WHERE grl.goodsReturn.invoiceId = :invoiceId " +
        "AND grl.goodsReturn.status = 'CONFIRMED' " +
        "AND grl.productId = :productId " +
        "AND grl.unitOfMeasure = :unitOfMeasure"
    )
    java.math.BigDecimal getTotalReturnedQuantity(@org.springframework.data.repository.query.Param("invoiceId") UUID invoiceId,
                                                  @org.springframework.data.repository.query.Param("productId") Long productId,
                                                  @org.springframework.data.repository.query.Param("unitOfMeasure") String unitOfMeasure);
    java.util.List<GoodsReturn> findByBranchId(Long branchId);

    /**
     * Đọc và khóa ghi phiếu (SELECT ... FOR UPDATE). Sửa nháp và xác nhận cùng khóa phiếu trước, nên hai thao tác
     * này không chạy xen nhau: xác nhận không thể tác động kho / công nợ theo nội dung đang bị sửa dở.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM GoodsReturn r WHERE r.id = :id")
    java.util.Optional<GoodsReturn> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
}
