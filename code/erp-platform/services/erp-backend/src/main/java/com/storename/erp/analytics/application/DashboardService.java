package com.storename.erp.analytics.application;

import com.storename.erp.analytics.api.dto.DashboardMetricsDto;
import com.storename.erp.analytics.api.dto.ProductPerformanceDto;
import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import com.storename.erp.analytics.infrastructure.FactSalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    
    private final AnalyticsDataPort analyticsDataPort;
    private final FactSalesRepository factSalesRepository;
    
    @Transactional(readOnly = true)
    public DashboardMetricsDto getDashboardMetrics(Long branchId, Integer startDateKey, Integer endDateKey) {
        log.info("Fetching dashboard metrics for branch: {}, startDate: {}, endDate: {}", branchId, startDateKey, endDateKey);
        
        BigDecimal overdueDebt = calculateTotalDebt(branchId);
        
        BigDecimal totalRevenue = factSalesRepository.getTotalRevenue(branchId, startDateKey, endDateKey);
        BigDecimal grossProfit = factSalesRepository.getTotalGrossProfit(branchId, startDateKey, endDateKey);
        BigDecimal cogs = factSalesRepository.getTotalCogs(branchId, startDateKey, endDateKey);
        List<ProductPerformanceDto> topSelling = factSalesRepository.getTopSellingProducts(branchId, startDateKey, endDateKey);

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
