package com.storename.erp.inventory.domain.event;

import org.springframework.context.ApplicationEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class InboundReceiptConfirmedEvent extends ApplicationEvent {

    private final UUID receiptId;
    private final UUID purchaseOrderId;
    private final Long branchId;

    public InboundReceiptConfirmedEvent(Object source, UUID receiptId, UUID purchaseOrderId, Long branchId) {
        super(source);
        this.receiptId = receiptId;
        this.purchaseOrderId = purchaseOrderId;
        this.branchId = branchId;
    }
}
