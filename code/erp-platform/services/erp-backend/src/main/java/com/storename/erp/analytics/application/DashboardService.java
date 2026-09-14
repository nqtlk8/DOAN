package com.storename.erp.analytics.application;

import com.storename.erp.analytics.api.dto.DashboardMetricsDto;
import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    
    private final AnalyticsDataPort analyticsDataPort;
    
    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetrics(Long branchId, Integer startDateKey, Integer endDateKey) {
        log.info("Fetching dashboard metrics for branch: {}, startDate: {}, endDate: {}", branchId, startDateKey, endDateKey);
        
        BigDecimal overdueDebt = calculateTotalDebt(branchId);
        
        BigDecimal totalRevenue = analyticsDataPort.getTotalRevenue(branchId, startDateKey, endDateKey);
        BigDecimal grossProfit = analyticsDataPort.getTotalGrossProfit(branchId, startDateKey, endDateKey);
        BigDecimal cogs = analyticsDataPort.getTotalCogs(branchId, startDateKey, endDateKey);
        List<com.storename.erp.analytics.api.dto.ProductPerformanceDto> topSelling = analyticsDataPort.getTopSellingProducts(branchId, startDateKey, endDateKey);

        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        if (grossProfit == null) grossProfit = BigDecimal.ZERO;
        if (cogs == null) cogs = BigDecimal.ZERO;

        BigDecimal inventoryTurnover = BigDecimal.ZERO; // Need more logic or leave as ZERO
        
        return DashboardMetricsDto.builder()
                .totalRevenue(totalRevenue)
                .grossProfit(grossProfit)
                .inventoryTurnoverRatio(inventoryTurnover)
                .totalOverdueDebt(overdueDebt)
                .topSellingProducts(topSelling)
                .build();
    }
    
    private BigDecimal calculateTotalDebt(Long branchId) {
        BigDecimal totalDebt = analyticsDataPort.getTotalReceivableDebt(branchId);
        return totalDebt != null ? totalDebt : BigDecimal.ZERO;
    }
}
