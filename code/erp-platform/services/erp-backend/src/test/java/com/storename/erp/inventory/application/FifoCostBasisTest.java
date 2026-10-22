package com.storename.erp.inventory.application;

import com.storename.erp.inventory.domain.CostLayer;
import com.storename.erp.inventory.infrastructure.CostLayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * costBasis của kết quả FIFO phản ánh nguồn thật của các lớp giá vốn đã tiêu thụ
 * (trước đây luôn gán cứng "NORMAL").
 */
@ExtendWith(MockitoExtension.class)
class FifoCostBasisTest {

    @Mock
    private CostLayerRepository costLayerRepo;

    @InjectMocks
    private FifoCostService fifoCostService;

    private static CostLayer inbound(String qty, String cost) {
        return CostLayer.fromInbound(1L, 1L, new BigDecimal(qty), new BigDecimal(cost), UUID.randomUUID());
    }

    private static CostLayer returned(String qty, String cost) {
        return CostLayer.fromReturn(1L, 1L, new BigDecimal(qty), new BigDecimal(cost), UUID.randomUUID());
    }

    @Test
    @DisplayName("Chỉ tiêu thụ lớp hàng trả -> costBasis = RETURN")
    void onlyReturnLayer_isReturn() {
        when(costLayerRepo.findAvailableForFifoWithLock(1L, 1L)).thenReturn(List.of(returned("5", "30")));

        FifoCostService.FifoResult r = fifoCostService.consume(1L, 1L, new BigDecimal("3"));

        assertThat(r.costBasis()).isEqualTo("RETURN");
        assertThat(r.unitCostSnapshot()).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("Lấy từ lớp RETURN rồi lớp NORMAL -> costBasis = MIXED, giá vốn bình quân theo SL")
    void returnThenInbound_isMixed() {
        when(costLayerRepo.findAvailableForFifoWithLock(1L, 1L))
                .thenReturn(List.of(returned("2", "30"), inbound("10", "40")));

        FifoCostService.FifoResult r = fifoCostService.consume(1L, 1L, new BigDecimal("5"));

        assertThat(r.costBasis()).isEqualTo(FifoCostService.MIXED);
        // (2 x 30 + 3 x 40) / 5 = 36
        assertThat(r.unitCostSnapshot()).isEqualByComparingTo("36");
    }

    @Test
    @DisplayName("Đủ hàng ngay ở lớp NORMAL đầu tiên -> lớp RETURN phía sau không làm đổi costBasis")
    void untouchedLaterLayer_doesNotAffectBasis() {
        when(costLayerRepo.findAvailableForFifoWithLock(1L, 1L))
                .thenReturn(List.of(inbound("10", "40"), returned("2", "30")));

        FifoCostService.FifoResult r = fifoCostService.consume(1L, 1L, new BigDecimal("4"));

        assertThat(r.costBasis()).isEqualTo("NORMAL");
    }

    @Test
    @DisplayName("Số lượng 4 chữ số thập phân được tiêu thụ chính xác")
    void fractionalQuantity_consumedExactly() {
        CostLayer layer = inbound("10.0004", "1000");
        when(costLayerRepo.findAvailableForFifoWithLock(1L, 1L)).thenReturn(List.of(layer));

        FifoCostService.FifoResult r = fifoCostService.consume(1L, 1L, new BigDecimal("10.0004"));

        assertThat(r.unconsumedQty()).isEqualByComparingTo("0");
        assertThat(layer.getRemainingQty()).isEqualByComparingTo("0");
    }
}
