package com.storename.erp.analytics.application;

import com.storename.erp.analytics.api.dto.DashboardMetricsDto;
import com.storename.erp.analytics.api.dto.ProductPerformanceDto;
import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AnalyticsDataPort analyticsDataPort;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboardMetrics_shouldReturnMappedDto_whenDataExists() {
        // Arrange
        Long branchId = 1L;
        Integer start = 20260901;
        Integer end = 20260930;

        when(analyticsDataPort.getTotalReceivableDebt(branchId)).thenReturn(BigDecimal.valueOf(5000));
        when(analyticsDataPort.getTotalRevenue(branchId, start, end)).thenReturn(BigDecimal.valueOf(10000));
        when(analyticsDataPort.getTotalGrossProfit(branchId, start, end)).thenReturn(BigDecimal.valueOf(4000));
        when(analyticsDataPort.getTotalCogs(branchId, start, end)).thenReturn(BigDecimal.valueOf(6000));

        ProductPerformanceDto topProduct = new ProductPerformanceDto();
        topProduct.setProductId(1L);
        topProduct.setProductName("Test Product");
        topProduct.setQuantitySold(BigDecimal.valueOf(10));
        topProduct.setRevenue(BigDecimal.valueOf(1000));
        
        when(analyticsDataPort.getTopSellingProducts(branchId, start, end)).thenReturn(List.of(topProduct));

        // Act
        DashboardMetricsDto result = dashboardService.getDashboardMetrics(branchId, start, end);

        // Assert
        assertThat(result.getTotalOverdueDebt()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(result.getGrossProfit()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        assertThat(result.getInventoryTurnoverRatio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTopSellingProducts()).hasSize(1);
        assertThat(result.getTopSellingProducts().get(0).getProductName()).isEqualTo("Test Product");
    }

    @Test
    void getDashboardMetrics_shouldHandleNullsFromPortAndReturnZeros() {
        // Arrange
        Long branchId = 1L;
        Integer start = 20260901;
        Integer end = 20260930;

        when(analyticsDataPort.getTotalReceivableDebt(branchId)).thenReturn(null);
        when(analyticsDataPort.getTotalRevenue(branchId, start, end)).thenReturn(null);
        when(analyticsDataPort.getTotalGrossProfit(branchId, start, end)).thenReturn(null);
        when(analyticsDataPort.getTotalCogs(branchId, start, end)).thenReturn(null);
        when(analyticsDataPort.getTopSellingProducts(branchId, start, end)).thenReturn(List.of());

        // Act
        DashboardMetricsDto result = dashboardService.getDashboardMetrics(branchId, start, end);

        // Assert
        assertThat(result.getTotalOverdueDebt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getGrossProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getInventoryTurnoverRatio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTopSellingProducts()).isEmpty();
    }
}
