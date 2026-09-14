package com.storename.erp.analytics.application;

import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import com.storename.erp.analytics.domain.InventoryAlertConfig;
import com.storename.erp.analytics.domain.InventoryAlertLog;
import com.storename.erp.analytics.infrastructure.InventoryAlertConfigRepository;
import com.storename.erp.analytics.infrastructure.InventoryAlertLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LowStockAlertJobTest {

    @Mock
    private InventoryAlertConfigRepository configRepository;

    @Mock
    private InventoryAlertLogRepository logRepository;

    @Mock
    private AnalyticsDataPort analyticsDataPort;

    @InjectMocks
    private LowStockAlertJob lowStockAlertJob;

    @Test
    void checkLowStock_whenCurrentStockIsNegative_shouldCreateNegativeStockAlert() {
        // Arrange
        InventoryAlertConfig config = new InventoryAlertConfig();
        config.setProductId(100L);
        config.setBranchId(1L);
        config.setMinQuantityThreshold(BigDecimal.valueOf(10));

        when(configRepository.findByIsActiveTrue()).thenReturn(List.of(config));
        when(analyticsDataPort.getCurrentStockQuantity(100L, 1L)).thenReturn(BigDecimal.valueOf(-5));
        when(logRepository.findByProductIdAndBranchIdAndStatus(100L, 1L, "ACTIVE")).thenReturn(Optional.empty());

        // Act
        lowStockAlertJob.checkLowStock();

        // Assert
        ArgumentCaptor<InventoryAlertLog> logCaptor = ArgumentCaptor.forClass(InventoryAlertLog.class);
        verify(logRepository, times(1)).save(logCaptor.capture());
        
        InventoryAlertLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getProductId()).isEqualTo(100L);
        assertThat(savedLog.getBranchId()).isEqualTo(1L);
        assertThat(savedLog.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void checkLowStock_whenCurrentStockIsBelowThreshold_shouldCreateLowStockAlert() {
        // Arrange
        InventoryAlertConfig config = new InventoryAlertConfig();
        config.setProductId(100L);
        config.setBranchId(1L);
        config.setMinQuantityThreshold(BigDecimal.valueOf(10));

        when(configRepository.findByIsActiveTrue()).thenReturn(List.of(config));
        when(analyticsDataPort.getCurrentStockQuantity(100L, 1L)).thenReturn(BigDecimal.valueOf(5));
        when(logRepository.findByProductIdAndBranchIdAndStatus(100L, 1L, "ACTIVE")).thenReturn(Optional.empty());

        // Act
        lowStockAlertJob.checkLowStock();

        // Assert
        ArgumentCaptor<InventoryAlertLog> logCaptor = ArgumentCaptor.forClass(InventoryAlertLog.class);
        verify(logRepository, times(1)).save(logCaptor.capture());
        
        InventoryAlertLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getProductId()).isEqualTo(100L);
        assertThat(savedLog.getBranchId()).isEqualTo(1L);
        assertThat(savedLog.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void checkLowStock_whenCurrentStockIsAboveThreshold_shouldResolveActiveAlert() {
        // Arrange
        InventoryAlertConfig config = new InventoryAlertConfig();
        config.setProductId(100L);
        config.setBranchId(1L);
        config.setMinQuantityThreshold(BigDecimal.valueOf(10));

        InventoryAlertLog activeLog = new InventoryAlertLog();
        activeLog.setProductId(100L);
        activeLog.setBranchId(1L);
        activeLog.setStatus("ACTIVE");

        when(configRepository.findByIsActiveTrue()).thenReturn(List.of(config));
        when(analyticsDataPort.getCurrentStockQuantity(100L, 1L)).thenReturn(BigDecimal.valueOf(15));
        when(logRepository.findByProductIdAndBranchIdAndStatus(100L, 1L, "ACTIVE")).thenReturn(Optional.of(activeLog));

        // Act
        lowStockAlertJob.checkLowStock();

        // Assert
        ArgumentCaptor<InventoryAlertLog> logCaptor = ArgumentCaptor.forClass(InventoryAlertLog.class);
        verify(logRepository, times(1)).save(logCaptor.capture());
        
        InventoryAlertLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getStatus()).isEqualTo("RESOLVED");
    }

    @Test
    void checkLowStock_whenNoActiveConfigs_shouldDoNothing() {
        // Arrange
        when(configRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

        // Act
        lowStockAlertJob.checkLowStock();

        // Assert
        verify(analyticsDataPort, never()).getCurrentStockQuantity(any(), any());
        verify(logRepository, never()).save(any());
    }
}
