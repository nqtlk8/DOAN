package com.storename.erp.inventory.domain;

import java.math.BigDecimal;

/**
 * Chiến lược tính giá vốn.
 */
public interface CostingStrategy {
    /**
     * Tính avgCost mới sau khi nhập hàng.
     * @param currentQty    tồn kho hiện tại TRƯỚC khi tăng
     * @param currentAvgCost  avgCost hiện tại
     * @param inboundQty    số lượng nhập vào
     * @param inboundUnitCost đơn giá nhập
     * @return avgCost mới
     */
    BigDecimal calculate(BigDecimal currentQty, BigDecimal currentAvgCost,
                         BigDecimal inboundQty, BigDecimal inboundUnitCost);
}
