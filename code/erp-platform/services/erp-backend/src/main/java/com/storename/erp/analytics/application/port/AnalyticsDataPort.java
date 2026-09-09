package com.storename.erp.analytics.application.port;

import java.math.BigDecimal;

public interface AnalyticsDataPort {
    BigDecimal getTotalReceivableDebt(Long branchId);
    BigDecimal getCurrentStockQuantity(Long productId, Long branchId);
}
