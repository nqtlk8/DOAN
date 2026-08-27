package com.storename.erp.inventory.domain;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tính giá vốn theo bình quân gia quyền.
 */
@Component
@ConditionalOnProperty(name = "erp.inventory.costing-strategy", havingValue = "weighted-average", matchIfMissing = true)
public class WeightedAverageCostingStrategy implements CostingStrategy {

    @Override
    public BigDecimal calculate(BigDecimal currentQty, BigDecimal currentAvgCost, 
                                BigDecimal inboundQty, BigDecimal inboundUnitCost) {
        BigDecimal totalQty = currentQty.add(inboundQty);
        if (totalQty.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentTotalCost = currentQty.multiply(currentAvgCost);
        BigDecimal inboundTotalCost = inboundQty.multiply(inboundUnitCost);
        
        return currentTotalCost.add(inboundTotalCost).divide(totalQty, 4, RoundingMode.HALF_UP);
    }
}
