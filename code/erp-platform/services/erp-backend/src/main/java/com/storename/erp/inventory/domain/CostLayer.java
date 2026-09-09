package com.storename.erp.inventory.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Lớp giá vốn FIFO. Khái niệm thuần kế toán — không phải "lô hàng" nghiệp vụ.
 * remaining_qty giảm dần theo FIFO khi bán.
 */
@Entity @Table(name = "cost_layer")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CostLayer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(name = "branch_id",  nullable = false) private Long branchId;

    @Column(name = "unit_cost",     nullable = false, precision = 18, scale = 4) private BigDecimal unitCost;
    @Column(name = "initial_qty",   nullable = false, precision = 18, scale = 3) private BigDecimal initialQty;
    @Column(name = "remaining_qty", nullable = false, precision = 18, scale = 3) private BigDecimal remainingQty;

    @Column(name = "inbound_movement_id") private Long inboundMovementId;
    @Column(name = "cost_basis", nullable = false, length = 20) private String costBasis = "NORMAL";

    @Version private Long version;  // optimistic lock cho FIFO consumption

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false) private ZonedDateTime createdAt;

    public static CostLayer fromInbound(Long pid, Long bid, BigDecimal qty,
                                        BigDecimal cost, Long movementId) {
        CostLayer l = new CostLayer();
        l.productId = pid; l.branchId = bid; l.unitCost = cost;
        l.initialQty = qty; l.remainingQty = qty;
        l.inboundMovementId = movementId; l.costBasis = "NORMAL";
        return l;
    }

    public static CostLayer fromReturn(Long pid, Long bid, BigDecimal qty,
                                       BigDecimal returnPrice, Long movementId) {
        CostLayer l = new CostLayer();
        l.productId = pid; l.branchId = bid; l.unitCost = returnPrice;
        l.initialQty = qty; l.remainingQty = qty;
        l.inboundMovementId = movementId; l.costBasis = "RETURN";
        return l;
    }

    /** Tiêu thụ FIFO: trả về số lượng thực tế lấy từ layer này */
    public BigDecimal consume(BigDecimal needed) {
        BigDecimal taken = this.remainingQty.min(needed);
        this.remainingQty = this.remainingQty.subtract(taken);
        return taken;
    }

    public boolean isExhausted() {
        return remainingQty.compareTo(BigDecimal.ZERO) == 0;
    }
}
