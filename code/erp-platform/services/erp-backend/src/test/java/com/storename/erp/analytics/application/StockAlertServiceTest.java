package com.storename.erp.analytics.application;

import com.storename.erp.analytics.api.dto.StockAlertSummaryDto;
import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import com.storename.erp.analytics.domain.InventoryAlertConfig;
import com.storename.erp.analytics.domain.StockAlertType;
import com.storename.erp.analytics.domain.StockLevel;
import com.storename.erp.analytics.infrastructure.InventoryAlertConfigRepository;
import com.storename.erp.branch.api.BranchFacade;
import com.storename.erp.catalog.api.CatalogFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockAlertServiceTest {

    @Mock
    private AnalyticsDataPort analyticsDataPort;

    @Mock
    private InventoryAlertConfigRepository inventoryAlertConfigRepository;

    @Mock
    private CatalogFacade catalogFacade;

    @Mock
    private BranchFacade branchFacade;

    @InjectMocks
    private StockAlertService stockAlertService;

    @Test
    void getStockAlerts_shouldReturnCorrectAlertsAndEnrichNames_withTimes1Verify() {
        // Arrange
        Long branchId = 1L;
        
        // Product 1: Tồn âm không có config -> NEGATIVE_STOCK
        StockLevel level1 = new StockLevel(1L, branchId, BigDecimal.valueOf(-5));
        
        // Product 2: Tồn âm có config (ngưỡng 10) -> NEGATIVE_STOCK
        StockLevel level2 = new StockLevel(2L, branchId, BigDecimal.valueOf(-2));
        InventoryAlertConfig config2 = new InventoryAlertConfig();
        config2.setProductId(2L);
        config2.setBranchId(branchId);
        config2.setMinQuantityThreshold(BigDecimal.valueOf(10));
        
        // Product 3: Tồn = ngưỡng -> LOW_STOCK
        StockLevel level3 = new StockLevel(3L, branchId, BigDecimal.valueOf(5));
        InventoryAlertConfig config3 = new InventoryAlertConfig();
        config3.setProductId(3L);
        config3.setBranchId(branchId);
        config3.setMinQuantityThreshold(BigDecimal.valueOf(5));
        
        // Product 4: Tồn > ngưỡng -> KHÔNG CẢNH BÁO
        StockLevel level4 = new StockLevel(4L, branchId, BigDecimal.valueOf(20));
        InventoryAlertConfig config4 = new InventoryAlertConfig();
        config4.setProductId(4L);
        config4.setBranchId(branchId);
        config4.setMinQuantityThreshold(BigDecimal.valueOf(10));
        
        // Product 5: Chưa có movement (tồn 0), có config ngưỡng 5 -> LOW_STOCK
        InventoryAlertConfig config5 = new InventoryAlertConfig();
        config5.setProductId(5L);
        config5.setBranchId(branchId);
        config5.setMinQuantityThreshold(BigDecimal.valueOf(5));

        when(analyticsDataPort.getStockLevels(branchId)).thenReturn(List.of(level1, level2, level3, level4));
        when(inventoryAlertConfigRepository.findByIsActiveTrueAndBranchId(branchId)).thenReturn(List.of(config2, config3, config4, config5));
        
        when(catalogFacade.getProductBasicInfo(anyCollection())).thenReturn(Map.of(
            1L, CatalogFacade.ProductBasicInfo.builder().code("P1").name("Prod 1").build(),
            2L, CatalogFacade.ProductBasicInfo.builder().code("P2").name("Prod 2").build(),
            3L, CatalogFacade.ProductBasicInfo.builder().code("P3").name("Prod 3").build(),
            5L, CatalogFacade.ProductBasicInfo.builder().code("P5").name("Prod 5").build()
        ));
        
        when(branchFacade.getBranchNames(anyCollection())).thenReturn(Map.of(1L, "Branch 1"));

        // Act
        StockAlertSummaryDto summary = stockAlertService.getStockAlerts(branchId);

        // Assert
        assertThat(summary.getNegativeCount()).isEqualTo(2);
        assertThat(summary.getLowStockCount()).isEqualTo(2);
        assertThat(summary.getAlerts()).hasSize(4);
        
        // Sort order: NEGATIVE first, then ascending qty
        assertThat(summary.getAlerts().get(0).getAlertType()).isEqualTo(StockAlertType.NEGATIVE_STOCK);
        assertThat(summary.getAlerts().get(0).getProductId()).isEqualTo(1L); // -5
        
        assertThat(summary.getAlerts().get(1).getAlertType()).isEqualTo(StockAlertType.NEGATIVE_STOCK);
        assertThat(summary.getAlerts().get(1).getProductId()).isEqualTo(2L); // -2
        
        assertThat(summary.getAlerts().get(2).getAlertType()).isEqualTo(StockAlertType.LOW_STOCK);
        assertThat(summary.getAlerts().get(2).getProductId()).isEqualTo(5L); // 0
        
        assertThat(summary.getAlerts().get(3).getAlertType()).isEqualTo(StockAlertType.LOW_STOCK);
        assertThat(summary.getAlerts().get(3).getProductId()).isEqualTo(3L); // 5

        verify(catalogFacade, times(1)).getProductBasicInfo(anyCollection());
        verify(branchFacade, times(1)).getBranchNames(anyCollection());
    }

    @Test
    void getStockAlerts_allBranches_shouldCallFindByIsActiveTrue() {
        when(analyticsDataPort.getStockLevels(null)).thenReturn(List.of());
        when(inventoryAlertConfigRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(catalogFacade.getProductBasicInfo(anyCollection())).thenReturn(Map.of());
        when(branchFacade.getBranchNames(anyCollection())).thenReturn(Map.of());

        stockAlertService.getStockAlerts(null);

        verify(inventoryAlertConfigRepository, times(1)).findByIsActiveTrue();
        verify(inventoryAlertConfigRepository, never()).findByIsActiveTrueAndBranchId(any());
    }
}
