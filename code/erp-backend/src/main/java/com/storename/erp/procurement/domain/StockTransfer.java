package com.storename.erp.procurement.domain;

import com.storename.erp.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stock_transfer")
@Getter
@Setter
public class StockTransfer extends BaseEntity {

    @Column(name = "from_branch_id", nullable = false)
    private UUID fromBranchId;

    @Column(name = "to_branch_id", nullable = false)
    private UUID toBranchId;

    @Column(name = "transfer_code", nullable = false, unique = true, length = 100)
    private String transferCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StockTransferStatus status = StockTransferStatus.REQUESTED;

    @Column(name = "outbound_receipt_id")
    private UUID outboundReceiptId;

    @Column(name = "inbound_receipt_id")
    private UUID inboundReceiptId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "stockTransfer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockTransferLine> lines = new ArrayList<>();

    public void addLine(StockTransferLine line) {
        lines.add(line);
        line.setStockTransfer(this);
    }

    public void ship(UUID outboundReceiptId) {
        if (this.status != StockTransferStatus.REQUESTED) {
            throw new IllegalStateException("Only REQUESTED transfer can be shipped");
        }
        this.status = StockTransferStatus.IN_TRANSIT;
        this.outboundReceiptId = outboundReceiptId;
    }

    public void receive(UUID inboundReceiptId) {
        if (this.status != StockTransferStatus.IN_TRANSIT) {
            throw new IllegalStateException("Only IN_TRANSIT transfer can be received");
        }
        this.status = StockTransferStatus.RECEIVED;
        this.inboundReceiptId = inboundReceiptId;
    }

    public void cancel() {
        if (this.status == StockTransferStatus.RECEIVED) {
            throw new IllegalStateException("Cannot cancel RECEIVED transfer");
        }
        this.status = StockTransferStatus.CANCELLED;
    }
}
