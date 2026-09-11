package com.storename.erp.inventory.application;

import com.storename.erp.inventory.domain.CostLayer;
import com.storename.erp.inventory.infrastructure.CostLayerRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FifoCostServiceTest {

    @Mock
    private CostLayerRepository costLayerRepo;

    @InjectMocks
    private FifoCostService fifoCostService;

    private CostLayer layer1;
    private CostLayer layer2;

    @BeforeEach
    void setUp() {
        layer1 = CostLayer.fromInbound(1L, 1L, new BigDecimal("100"), new BigDecimal("10000"), UUID.randomUUID());
        layer2 = CostLayer.fromInbound(1L, 1L, new BigDecimal("100"), new BigDecimal("12000"), UUID.randomUUID());
    }

    @Test
    @DisplayName("consume 80 từ layer1[remaining=100, cost=10k] → snapshot=10k, costBasis=NORMAL")
    void testConsume_SingleLayer() {
        when(costLayerRepo.findAvailableForFifoWithLock(1L, 1L)).thenReturn(List.of(layer1));

        FifoCostService.FifoResult result = fifoCostService.consume(1L, 1L, new BigDecimal("80"));

        assertThat(result.unitCostSnapshot()).isEqualByComparingTo("10000");
        assertThat(result.costBasis()).isEqualTo("NORMAL");
        assertThat(result.unconsumedQty()).isEqualByComparingTo("0");
        assertThat(layer1.getRemainingQty()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("consume 150 từ [layer1(100,10k), layer2(100,12k)] → snapshot trung bình")
    void testConsume_MultipleLayers() {
        when(costLayerRepo.findAvailableForFifoWithLock(1L, 1L)).thenReturn(Arrays.asList(layer1, layer2));

        FifoCostService.FifoResult result = fifoCostService.consume(1L, 1L, new BigDecimal("150"));

        // Cost = (100 * 10000 + 50 * 12000) / 150 = 1600000 / 150 = 10666.6667
        assertThat(result.unitCostSnapshot()).isEqualByComparingTo("10666.6667");
        assertThat(result.costBasis()).isEqualTo("NORMAL");
        assertThat(result.unconsumedQty()).isEqualByComparingTo("0");
        assertThat(layer1.getRemainingQty()).isEqualByComparingTo("0");
        assertThat(layer2.getRemainingQty()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("consume 50 khi layers rỗng → costBasis='NO_LAYER', snapshot=0")
    void testConsume_NoLayer_NegativeStock() {
        when(costLayerRepo.findAvailableForFifoWithLock(1L, 1L)).thenReturn(Collections.emptyList());

        FifoCostService.FifoResult result = fifoCostService.consume(1L, 1L, new BigDecimal("50"));

        assertThat(result.unitCostSnapshot()).isEqualByComparingTo("0");
        assertThat(result.costBasis()).isEqualTo("NO_LAYER");
        assertThat(result.unconsumedQty()).isEqualByComparingTo("50");
    }
}
