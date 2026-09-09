package com.storename.erp.inventory.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Nguồn sự thật về số lượng tồn kho. Append-only — không UPDATE/DELETE.
 * Invariant: SUM(quantity) GROUP BY product_id,branch_id = stock_on_hand.quantity
 */
@Entity
@Table(name = "stock_movement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(name = "branch_id",  nullable = false) private Long branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    /** Dương = nhập vào, âm = xuất ra */
    @Column(nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "ref_type", nullable = false, length = 50)  private String refType;
    @Column(name = "ref_id",   nullable = false, length = 100) private String refId;
    @Column(name = "ref_line_id")  private UUID refLineId;
    @Column(name = "performed_by") private UUID performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    // --- Static factories ---
    public static StockMovement inbound(Long productId, Long branchId, BigDecimal qty,
                                        String receiptId, UUID lineId, UUID userId) {
        return build(productId, branchId, MovementType.INBOUND,  qty.abs(),
                     "inbound_receipt", receiptId, lineId, userId);
    }
    public static StockMovement sale(Long productId, Long branchId, BigDecimal qty,
                                     String invoiceId, UUID lineId, UUID userId) {
        return build(productId, branchId, MovementType.SALE, qty.abs().negate(),
                     "sales_invoice", invoiceId, lineId, userId);
    }
    public static StockMovement returnGoods(Long productId, Long branchId, BigDecimal qty,
                                            String returnId, UUID lineId, UUID userId) {
        return build(productId, branchId, MovementType.RETURN, qty.abs(),
                     "goods_return", returnId, lineId, userId);
    }
    private static StockMovement build(Long pid, Long bid, MovementType type, BigDecimal qty,
                                       String refType, String refId, UUID refLineId, UUID userId) {
        StockMovement m = new StockMovement();
        m.productId = pid; m.branchId = bid; m.movementType = type; m.quantity = qty;
        m.refType = refType; m.refId = refId; m.refLineId = refLineId; m.performedBy = userId;
        return m;
    }
}
