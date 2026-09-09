package com.storename.erp.inventory.domain;

import com.storename.erp.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Bảng tồn kho tức thời — 1 row = 1 sản phẩm × 1 chi nhánh.
 * Kế thừa BaseEntity (id UUID, @Version Integer, createdAt/updatedAt, isDeleted).
 */
@Entity
@Table(name = "stock_on_hand",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_stock_product_branch",
           columnNames = {"product_id", "branch_id"}))
@Getter
public class StockOnHand extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;


    protected StockOnHand() {}

    public StockOnHand(Long productId, Long branchId) {
        this.productId = productId;
        this.branchId = branchId;
    }

    /** Tăng tồn kho. */
    public void increase(BigDecimal qty, String reason) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("qty must be positive");
        this.quantity = this.quantity.add(qty);
    }

    /** Giảm tồn kho. Throws StockInsufficientException nếu không đủ. */
    public void decrease(BigDecimal qty, String reason) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("qty must be positive");
        if (this.quantity.compareTo(qty) < 0)
            throw new StockInsufficientException(this.productId, this.branchId,
                                                  this.quantity, qty);
        this.quantity = this.quantity.subtract(qty);
    }

    /** Giảm tồn kho cho phép âm (bán khống). */
    public void decreaseAllowNegative(BigDecimal qty, String reason) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("qty must be positive");
        this.quantity = this.quantity.subtract(qty);
    }

}
