package com.storename.erp.order.domain;

import com.storename.erp.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "goods_return_line")
@Getter
@Setter
public class GoodsReturnLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private GoodsReturn goodsReturn;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    /** Giá vốn của hàng trả (snapshot lúc xác nhận phiếu trả) — không phải giá bán. */
    @Column(name = "unit_cost", precision = 19, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure;
}
