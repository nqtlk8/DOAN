package com.storename.erp.order.infrastructure;

import com.storename.erp.order.domain.SalesInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, UUID> {
    java.util.List<SalesInvoice> findByBranchId(Long branchId);

    boolean existsByInvoiceCode(String invoiceCode);

    /** Chứng từ đã tạo với khóa lập phiếu này (gửi lại cùng Idempotency-Key -> trả lại chứng từ cũ). */
    java.util.Optional<SalesInvoice> findByRequestKey(String requestKey);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT i FROM SalesInvoice i WHERE i.id = :id")
    Optional<SalesInvoice> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    /**
     * Giá vốn đã snapshot của các lần bán sản phẩm cho khách tại chi nhánh, mới nhất trước
     * (dùng Pageable.ofSize(1) để lấy lần gần nhất).
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT l.unitCost FROM SalesInvoiceLine l JOIN l.invoice i
            WHERE i.customerId = :customerId AND i.branchId = :branchId AND l.productId = :productId
              AND i.status = com.storename.erp.order.domain.SalesInvoiceStatus.CONFIRMED
              AND l.unitCost IS NOT NULL
            ORDER BY i.confirmedAt DESC
            """)
    java.util.List<java.math.BigDecimal> findRecentSaleUnitCosts(
            @org.springframework.data.repository.query.Param("customerId") UUID customerId,
            @org.springframework.data.repository.query.Param("branchId") Long branchId,
            @org.springframework.data.repository.query.Param("productId") Long productId,
            org.springframework.data.domain.Pageable pageable);
}
