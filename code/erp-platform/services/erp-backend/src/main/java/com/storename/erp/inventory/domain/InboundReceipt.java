package com.storename.erp.inventory.domain;

import com.storename.erp.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Phiáº¿u nháº­p kho.
 */
@Entity
@Table(name = "inbound_receipt")
@Getter
public class InboundReceipt extends BaseEntity {

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "receipt_code", nullable = false, unique = true)
    private String receiptCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceiptStatus status = ReceiptStatus.DRAFT;

    @Setter
    @Column(name = "supplier_id", length = 200)
    private java.util.UUID supplierId;

    @Column(name = "note")
    private String note;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InboundReceiptLine> lines = new ArrayList<>();

    protected InboundReceipt() {}

    public InboundReceipt(Long branchId, String receiptCode, String note) {
        this.branchId = branchId;
        this.receiptCode = receiptCode;
        this.note = note;
    }

    public void addLine(InboundReceiptLine line) {
        lines.add(line);
        line.setReceipt(this);
    }

    public void confirm(UUID userId) {
        if (this.status == ReceiptStatus.CONFIRMED) {
            throw new IllegalStateException("Receipt is already CONFIRMED");
        }
        this.status = ReceiptStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.confirmedBy = userId;
    }
}

