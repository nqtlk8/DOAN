package com.storename.erp.inventory.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event phát ra khi tồn kho giảm.
 */
@Getter
public class StockDecreasedEvent extends ApplicationEvent {

    private final UUID productId;
    private final UUID branchId;
    private final BigDecimal quantity;
    private final BigDecimal avgCostAfter;

    public StockDecreasedEvent(Object source, UUID productId, UUID branchId, BigDecimal quantity, BigDecimal avgCostAfter) {
        super(source);
        this.productId = productId;
        this.branchId = branchId;
        this.quantity = quantity;
        this.avgCostAfter = avgCostAfter;
    }
}
