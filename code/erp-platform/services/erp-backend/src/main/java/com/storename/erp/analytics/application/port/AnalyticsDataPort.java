package com.storename.erp.analytics.application.port;

import com.storename.erp.analytics.api.dto.ProductPerformanceDto;
import java.math.BigDecimal;
import java.util.List;

public interface AnalyticsDataPort {
    BigDecimal getTotalReceivableDebt(Long branchId);
    BigDecimal getCurrentStockQuantity(Long productId, Long branchId);
    List<ProductPerformanceDto> getTopSellingProducts(Long branchId, Integer startDateKey, Integer endDateKey);
    BigDecimal getTotalRevenue(Long branchId, Integer startDateKey, Integer endDateKey);
    BigDecimal getTotalGrossProfit(Long branchId, Integer startDateKey, Integer endDateKey);
    BigDecimal getTotalCogs(Long branchId, Integer startDateKey, Integer endDateKey);
}
