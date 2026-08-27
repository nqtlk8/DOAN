package com.storename.erp.inventory.domain;

import com.storename.erp.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Chi tiết phiếu nhập kho.
 */
@Entity
@Table(name = "inbound_receipt_line")
@Getter
public class InboundReceiptLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    @Setter
    private InboundReceipt receipt;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "unit_of_measure", nullable = false, length = 50)
    private String unitOfMeasure;

    protected InboundReceiptLine() {}

    public InboundReceiptLine(Long productId, BigDecimal quantity, BigDecimal unitCost, String unitOfMeasure) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.unitOfMeasure = unitOfMeasure;
    }
}
