package com.storename.erp.inventory.application;

import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.inventory.domain.CostLayer;
import com.storename.erp.inventory.domain.StockMovement;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.CostLayerRepository;
import com.storename.erp.inventory.infrastructure.StockMovementRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryFacadeImplTest {

    @Mock
    private StockOnHandRepository stockOnHandRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private CostLayerRepository costLayerRepository;
    @Mock
    private FifoCostService fifoCostService;

    @InjectMocks
    private InventoryFacadeImpl inventoryFacade;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getAvailableQuantity_ShouldReturnQuantityIfExists() {
        StockOnHand stock = new StockOnHand(1L, 100L);
        stock.increase(new BigDecimal("50.0"), "TEST");
        when(stockOnHandRepository.findByProductIdAndBranchId(1L, 100L)).thenReturn(Optional.of(stock));

        BigDecimal qty = inventoryFacade.getAvailableQuantity(1L, 100L);

        assertEquals(0, new BigDecimal("50.0").compareTo(qty));
    }

    @Test
    void getAvailableQuantity_ShouldReturnZeroIfNotExists() {
        when(stockOnHandRepository.findByProductIdAndBranchId(1L, 100L)).thenReturn(Optional.empty());

        BigDecimal qty = inventoryFacade.getAvailableQuantity(1L, 100L);

        assertEquals(0, BigDecimal.ZERO.compareTo(qty));
    }

    @Test
    void recordSaleAndGetCost_ShouldDecreaseStockAndSaveMovement() {
        Long productId = 1L;
        Long branchId = 100L;
        BigDecimal qty = new BigDecimal("5.0");
        String invoiceId = "INV-01";
        UUID lineId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        FifoCostService.FifoResult mockResult = new FifoCostService.FifoResult(new BigDecimal("100.0"), "FIFO", java.util.Collections.emptyList(), BigDecimal.ZERO);
        when(fifoCostService.consume(productId, branchId, qty)).thenReturn(mockResult);

        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("10.0"), "INIT");
        when(stockOnHandRepository.findByProductIdAndBranchId(productId, branchId)).thenReturn(Optional.of(stock));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement m = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
            return m;
        });

        InventoryFacade.SaleCostResult result = inventoryFacade.recordSaleAndGetCost(productId, branchId, qty, invoiceId, lineId, userId);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("100.0").compareTo(result.unitCostSnapshot()));
        assertEquals("FIFO", result.costBasis());

        assertEquals(0, new BigDecimal("5.0").compareTo(stock.getQuantity())); // 10 - 5
        verify(stockOnHandRepository).save(stock);
        verify(stockMovementRepository).save(argThat(m -> m.getMovementType().name().equals("SALE")));
    }

    @Test
    void recordReturn_ShouldIncreaseStockAndSaveCostLayer() {
        Long productId = 1L;
        Long branchId = 100L;
        BigDecimal qty = new BigDecimal("3.0");
        BigDecimal returnPrice = new BigDecimal("110.0");
        String returnId = "RET-01";
        UUID lineId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        StockOnHand stock = new StockOnHand(productId, branchId);
        when(stockOnHandRepository.findByProductIdAndBranchId(productId, branchId)).thenReturn(Optional.of(stock));
        
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement m = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
            return m;
        });

        inventoryFacade.recordReturn(productId, branchId, qty, returnPrice, returnId, lineId, userId);

        assertEquals(0, new BigDecimal("3.0").compareTo(stock.getQuantity()));
        verify(stockOnHandRepository).save(stock);
        verify(stockMovementRepository).save(argThat(m -> m.getMovementType().name().equals("RETURN")));
        verify(costLayerRepository).save(argThat(layer -> 
                layer.getInitialQty().compareTo(qty) == 0 &&
                layer.getUnitCost().compareTo(returnPrice) == 0
        ));
    }
}
