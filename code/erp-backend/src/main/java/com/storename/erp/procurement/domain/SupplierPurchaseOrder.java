package com.storename.erp.procurement.domain;

import com.storename.erp.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "supplier_purchase_order")
@Getter
@Setter
public class SupplierPurchaseOrder extends BaseEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "po_code", nullable = false, unique = true, length = 100)
    private String poCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierPurchaseOrderLine> lines = new ArrayList<>();

    public void addLine(SupplierPurchaseOrderLine line) {
        lines.add(line);
        line.setPurchaseOrder(this);
    }

    public void calculateTotal() {
        this.totalAmount = lines.stream()
                .map(line -> line.getQuantity().multiply(line.getUnitCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void confirm() {
        if (this.status != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT PO can be confirmed");
        }
        this.status = PurchaseOrderStatus.CONFIRMED;
    }

    public void receive() {
        if (this.status != PurchaseOrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED PO can be received");
        }
        this.status = PurchaseOrderStatus.RECEIVED;
    }
    
    public void cancel() {
        if (this.status == PurchaseOrderStatus.RECEIVED) {
            throw new IllegalStateException("Cannot cancel RECEIVED PO");
        }
        this.status = PurchaseOrderStatus.CANCELLED;
    }
}
