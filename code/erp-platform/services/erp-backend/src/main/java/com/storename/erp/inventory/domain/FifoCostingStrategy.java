package com.storename.erp.inventory.domain;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Tính giá vốn theo FIFO.
 * Sprint 2.1: stub implementation, return inboundUnitCost.
 */
@Component("fifoCostingStrategy")
@ConditionalOnProperty(name = "erp.inventory.costing-strategy", havingValue = "fifo")
public class FifoCostingStrategy implements CostingStrategy {

    @Override
    public BigDecimal calculate(BigDecimal currentQty, BigDecimal currentAvgCost, 
                                BigDecimal inboundQty, BigDecimal inboundUnitCost) {
        // Sprint 2.1 stub
        return inboundUnitCost;
    }
}
