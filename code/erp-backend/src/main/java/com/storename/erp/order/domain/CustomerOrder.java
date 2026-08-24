package com.storename.erp.order.domain;

import com.storename.erp.common.domain.BaseEntity;
import com.storename.erp.crm.domain.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customer_order")
@Getter
@Setter
public class CustomerOrder extends BaseEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerOrderStatus status = CustomerOrderStatus.DRAFT;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "note")
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerOrderLine> lines = new ArrayList<>();

    public void addLine(CustomerOrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }

    public void calculateTotal() {
        this.totalAmount = lines.stream()
                .map(l -> l.getUnitPrice().multiply(l.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void confirm() {
        if (this.status != CustomerOrderStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT order can be confirmed");
        }
        this.status = CustomerOrderStatus.CONFIRMED;
    }
    
    public void markAsInvoiced() {
        if (this.status != CustomerOrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED order can be invoiced");
        }
        this.status = CustomerOrderStatus.INVOICED;
    }
}
