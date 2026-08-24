package com.storename.erp.order.domain;

import com.storename.erp.common.domain.BaseEntity;
import com.storename.erp.crm.domain.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customer_order_line")
@Getter
@Setter
public class CustomerOrderLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure;
}
