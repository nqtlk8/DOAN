package com.storename.erp.inventory.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event phát ra khi tồn kho tăng.
 */
@Getter
public class StockIncreasedEvent extends ApplicationEvent {

    private final Long productId;
    private final Long branchId;
    private final BigDecimal quantity;
    private final BigDecimal avgCostAfter;

    public StockIncreasedEvent(Object source, Long productId, Long branchId, BigDecimal quantity, BigDecimal avgCostAfter) {
        super(source);
        this.productId = productId;
        this.branchId = branchId;
        this.quantity = quantity;
        this.avgCostAfter = avgCostAfter;
    }
}
