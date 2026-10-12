package com.storename.erp.order.domain;

import com.storename.erp.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Giá bán riêng của một khách cho một sản phẩm tại một chi nhánh (giá HIỆN HÀNH, dùng cho đơn sau).
 * Khác với sales_invoice_line.unit_price là SNAPSHOT bất biến của từng hóa đơn.
 * Được tạo/cập nhật tự động khi xác nhận hóa đơn có giá khác giá đang áp dụng
 * (xem CustomerProductPriceService#applyFromInvoice).
 */
@Entity
@Table(name = "customer_product_price", uniqueConstraints = @UniqueConstraint(
        name = "uk_cust_prod_branch",
        columnNames = {"customer_id", "product_id", "branch_id"}))
@Getter
@Setter
public class CustomerProductPrice extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    /** Hóa đơn gần nhất đã tạo/cập nhật giá này. */
    @Column(name = "source_invoice_id")
    private UUID sourceInvoiceId;

    /** Còn hiệu lực tại thời điểm {@code at}: chưa xóa, effective_from <= at < effective_to (null = vô thời hạn). */
    public boolean isActiveAt(LocalDateTime at) {
        if (isDeleted() || unitPrice == null) return false;
        if (effectiveFrom != null && effectiveFrom.isAfter(at)) return false;
        return effectiveTo == null || effectiveTo.isAfter(at);
    }
}
