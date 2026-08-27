package com.storename.erp.inventory.unit;

import com.storename.erp.inventory.domain.FifoCostingStrategy;
import com.storename.erp.inventory.domain.WeightedAverageCostingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CostingStrategyUnitTest {

    @Test
    public void testCostingStrategyCrossCheck() {
        WeightedAverageCostingStrategy weightedStrategy = new WeightedAverageCostingStrategy();
        FifoCostingStrategy fifoStrategy = new FifoCostingStrategy();

        // Inbound 1: qty = 10, unitCost = 100,000
        BigDecimal inbound1Qty = new BigDecimal("10");
        BigDecimal inbound1Cost = new BigDecimal("100000");

        BigDecimal currentQty = BigDecimal.ZERO;
        BigDecimal currentAvgCost = BigDecimal.ZERO;

        BigDecimal weightedCost1 = weightedStrategy.calculate(currentQty, currentAvgCost, inbound1Qty, inbound1Cost);
        BigDecimal fifoCost1 = fifoStrategy.calculate(currentQty, currentAvgCost, inbound1Qty, inbound1Cost);

        assertEquals(new BigDecimal("100000.0000"), weightedCost1);
        assertEquals(new BigDecimal("100000"), fifoCost1);

        // Update state after inbound 1
        currentQty = inbound1Qty;

        // Inbound 2: qty = 5, unitCost = 130,000
        BigDecimal inbound2Qty = new BigDecimal("5");
        BigDecimal inbound2Cost = new BigDecimal("130000");

        BigDecimal weightedCost2 = weightedStrategy.calculate(currentQty, weightedCost1, inbound2Qty, inbound2Cost);
        BigDecimal fifoCost2 = fifoStrategy.calculate(currentQty, fifoCost1, inbound2Qty, inbound2Cost);

        // Weighted Average expected: (10 * 100,000 + 5 * 130,000) / 15 = 1,650,000 / 15 = 110,000
        assertEquals(new BigDecimal("110000.0000"), weightedCost2);
        
        // FIFO (stub) expected: returns inboundUnitCost (130,000)
        assertEquals(new BigDecimal("130000"), fifoCost2);

        // Assert different
        assertNotEquals(weightedCost2, fifoCost2);
    }
}
