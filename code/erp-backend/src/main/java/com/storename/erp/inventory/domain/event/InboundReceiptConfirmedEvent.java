package com.storename.erp.inventory.domain.event;

import org.springframework.context.ApplicationEvent;
import lombok.Getter;

import java.util.UUID;

@Getter
public class InboundReceiptConfirmedEvent extends ApplicationEvent {

    private final UUID receiptId;
    private final UUID purchaseOrderId;
    private final UUID branchId;

    public InboundReceiptConfirmedEvent(Object source, UUID receiptId, UUID purchaseOrderId, UUID branchId) {
        super(source);
        this.receiptId = receiptId;
        this.purchaseOrderId = purchaseOrderId;
        this.branchId = branchId;
    }
}
