package com.storename.erp.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Lịch sử thay đổi giá riêng theo khách. Append-only. */
@Entity
@Table(name = "customer_product_price_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerProductPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "old_price", precision = 19, scale = 4)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal newPrice;

    @Column(name = "list_price", precision = 19, scale = 4)
    private BigDecimal listPrice;

    @Column(name = "source_invoice_id")
    private UUID sourceInvoiceId;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public static CustomerProductPriceHistory of(UUID customerId, Long productId, Long branchId,
                                                 BigDecimal oldPrice, BigDecimal newPrice, BigDecimal listPrice,
                                                 UUID sourceInvoiceId, UUID changedBy, LocalDateTime changedAt) {
        CustomerProductPriceHistory h = new CustomerProductPriceHistory();
        h.customerId = customerId;
        h.productId = productId;
        h.branchId = branchId;
        h.oldPrice = oldPrice;
        h.newPrice = newPrice;
        h.listPrice = listPrice;
        h.sourceInvoiceId = sourceInvoiceId;
        h.changedBy = changedBy;
        h.changedAt = changedAt;
        return h;
    }
}
