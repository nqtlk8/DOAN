package com.storename.erp.analytics.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardMetricsDto {
    private BigDecimal totalRevenue;
    private BigDecimal grossProfit;
    private BigDecimal inventoryTurnoverRatio;
    private BigDecimal totalOverdueDebt;
    private List<ProductPerformanceDto> topSellingProducts;
    private List<ProductPerformanceDto> slowMovingProducts;
}
