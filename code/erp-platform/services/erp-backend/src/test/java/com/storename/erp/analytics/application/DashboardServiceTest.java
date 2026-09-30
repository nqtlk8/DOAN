package com.storename.erp.analytics.application;

import com.storename.erp.analytics.AnalyticsProperties;
import com.storename.erp.analytics.api.dto.DashboardMetricsDto;
import com.storename.erp.analytics.api.dto.ProductPerformanceDto;
import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import com.storename.erp.analytics.domain.SalesTotals;
import com.storename.erp.analytics.domain.StockLevel;
import com.storename.erp.catalog.api.CatalogFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AnalyticsDataPort analyticsDataPort;

    @Mock
    private AnalyticsProperties analyticsProperties;
    
    @Mock
    private CatalogFacade catalogFacade;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(analyticsProperties.getBusinessZone()).thenReturn(ZoneId.of("Asia/Ho_Chi_Minh"));
        org.mockito.Mockito.lenient().when(analyticsProperties.getStorageZone()).thenReturn(ZoneId.of("UTC"));
    }

    @Test
    void getDashboardMetrics_shouldReturnMappedDto_whenDataExists() {
        // Arrange
        Long branchId = 1L;
        Integer start = 20260901;
        Integer end = 20260930;

        when(analyticsDataPort.getTotalReceivableDebt(branchId)).thenReturn(BigDecimal.valueOf(5000));
        when(analyticsDataPort.getSalesTotals(eq(branchId), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new SalesTotals(BigDecimal.valueOf(10000), BigDecimal.valueOf(6000)));

        ProductPerformanceDto topProduct = new ProductPerformanceDto();
        topProduct.setProductId(1L);
        topProduct.setProductName("Test Product");
        topProduct.setQuantitySold(BigDecimal.valueOf(10));
        topProduct.setRevenue(BigDecimal.valueOf(1000));
        
        when(analyticsDataPort.getTopProductsByRevenue(eq(branchId), any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(List.of(topProduct));
        
        when(analyticsDataPort.getInventoryValue(branchId)).thenReturn(BigDecimal.valueOf(12000));
        
        when(analyticsDataPort.getStockLevels(branchId)).thenReturn(List.of(new StockLevel(1L, branchId, BigDecimal.valueOf(20))));
        when(analyticsDataPort.getProductIdsSoldInPeriod(eq(branchId), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        
        when(catalogFacade.getProductBasicInfo(anyCollection())).thenReturn(Map.of(1L, CatalogFacade.ProductBasicInfo.builder().code("P1").name("Prod 1").build()));

        // Act
        DashboardMetricsDto result = dashboardService.getDashboardMetrics(branchId, start, end);

        // Assert
        assertThat(result.getTotalReceivableDebt()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(result.getGrossProfit()).isEqualByComparingTo(BigDecimal.valueOf(4000));
        assertThat(result.getInventoryTurnoverRatio()).isEqualByComparingTo(BigDecimal.valueOf(0.50)); // 6000 / 12000
        assertThat(result.getTopSellingProducts()).hasSize(1);
        assertThat(result.getTopSellingProducts().get(0).getProductName()).isEqualTo("Test Product");
        
        assertThat(result.getSlowMovingProducts()).hasSize(1);
        assertThat(result.getSlowMovingProducts().get(0).getProductName()).isEqualTo("Prod 1");
        assertThat(result.getSlowMovingProducts().get(0).getCurrentStock()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    void getDashboardMetrics_shouldHandleNullsFromPortAndReturnZeros() {
        // Arrange
        Long branchId = 1L;
        Integer start = 20260901;
        Integer end = 20260930;

        when(analyticsDataPort.getTotalReceivableDebt(branchId)).thenReturn(null);
        when(analyticsDataPort.getSalesTotals(eq(branchId), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(new SalesTotals(null, null));
        when(analyticsDataPort.getTopProductsByRevenue(eq(branchId), any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(List.of());
        when(analyticsDataPort.getInventoryValue(branchId)).thenReturn(BigDecimal.ZERO);
        
        when(analyticsDataPort.getStockLevels(branchId)).thenReturn(List.of());
        when(analyticsDataPort.getProductIdsSoldInPeriod(eq(branchId), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());
        when(catalogFacade.getProductBasicInfo(anyCollection())).thenReturn(Map.of());

        // Act
        DashboardMetricsDto result = dashboardService.getDashboardMetrics(branchId, start, end);

        // Assert
        assertThat(result.getTotalReceivableDebt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getGrossProfit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getInventoryTurnoverRatio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTopSellingProducts()).isEmpty();
        assertThat(result.getSlowMovingProducts()).isEmpty();
    }

    @Test
    void getDashboardMetrics_shouldThrowException_whenStartDateGreaterThanEndDate() {
        assertThatThrownBy(() -> dashboardService.getDashboardMetrics(1L, 20260930, 20260901))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("startDateKey must be less than or equal to endDateKey");
    }
    
    @Test
    void getDashboardMetrics_shouldThrowException_whenDateIsInvalid() {
        assertThatThrownBy(() -> dashboardService.getDashboardMetrics(1L, 20260231, 20260930))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid date format for startDateKey or endDateKey");
    }

    /**
     * M-06: ngày nghiệp vụ tính theo giờ Việt Nam, còn {@code confirmed_at} được ghi theo giờ JVM (Docker = UTC).
     * Ngày 02/09/2026 giờ VN = [2026-09-01 17:00, 2026-09-02 17:00) theo UTC; mốc cuối là khoảng mở.
     */
    @Test
    void getDashboardMetrics_shouldConvertBusinessDayToStorageZoneRange() {
        Long branchId = 1L;
        when(analyticsDataPort.getSalesTotals(eq(branchId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new SalesTotals(BigDecimal.ZERO, BigDecimal.ZERO));
        when(analyticsDataPort.getTopProductsByRevenue(eq(branchId), any(LocalDateTime.class), any(LocalDateTime.class), eq(10)))
                .thenReturn(List.of());
        when(analyticsDataPort.getStockLevels(branchId)).thenReturn(List.of());
        when(analyticsDataPort.getProductIdsSoldInPeriod(eq(branchId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(catalogFacade.getProductBasicInfo(anyCollection())).thenReturn(Map.of());

        dashboardService.getDashboardMetrics(branchId, 20260902, 20260902);

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(analyticsDataPort).getSalesTotals(eq(branchId), fromCaptor.capture(), toCaptor.capture());
        assertThat(fromCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 9, 1, 17, 0));
        assertThat(toCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 9, 2, 17, 0));
    }
}
