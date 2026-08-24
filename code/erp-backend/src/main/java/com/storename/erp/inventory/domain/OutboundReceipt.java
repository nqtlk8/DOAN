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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Phiếu xuất kho.
 */
@Entity
@Table(name = "outbound_receipt")
@Getter
public class OutboundReceipt extends BaseEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "receipt_code", nullable = false, unique = true)
    private String receiptCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceiptStatus status = ReceiptStatus.DRAFT;

    @Column(name = "note")
    private String note;
    
    @Column(name = "reason")
    private String reason;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OutboundReceiptLine> lines = new ArrayList<>();

    protected OutboundReceipt() {}

    public OutboundReceipt(UUID branchId, String receiptCode, String reason, String note) {
        this.branchId = branchId;
        this.receiptCode = receiptCode;
        this.reason = reason;
        this.note = note;
    }

    public void addLine(OutboundReceiptLine line) {
        lines.add(line);
        line.setReceipt(this);
    }

    /** Xác nhận phiếu xuất. */
    public void confirm(UUID userId) {
        if (this.status == ReceiptStatus.CONFIRMED) {
            throw new IllegalStateException("Receipt is already CONFIRMED");
        }
        this.status = ReceiptStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.confirmedBy = userId;
    }
}
