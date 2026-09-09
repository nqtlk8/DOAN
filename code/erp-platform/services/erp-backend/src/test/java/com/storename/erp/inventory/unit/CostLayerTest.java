package com.storename.erp.inventory.unit;

import com.storename.erp.inventory.domain.CostLayer;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

public class CostLayerTest {

    @Test
    public void consume_partialLayer() {
        CostLayer layer = CostLayer.fromInbound(1L, 1L, new BigDecimal("100"), new BigDecimal("10000"), 1L);
        BigDecimal taken = layer.consume(new BigDecimal("30"));
        assertThat(taken).isEqualByComparingTo("30");
        assertThat(layer.getRemainingQty()).isEqualByComparingTo("70");
        assertThat(layer.isExhausted()).isFalse();
    }

    @Test
    public void consume_exactLayer() {
        CostLayer layer = CostLayer.fromInbound(1L, 1L, new BigDecimal("50"), new BigDecimal("10000"), 2L);
        BigDecimal taken = layer.consume(new BigDecimal("50"));
        assertThat(taken).isEqualByComparingTo("50");
        assertThat(layer.getRemainingQty()).isEqualByComparingTo("0");
        assertThat(layer.isExhausted()).isTrue();
    }

    @Test
    public void consume_moreThanAvailable() {
        CostLayer layer = CostLayer.fromInbound(1L, 1L, new BigDecimal("20"), new BigDecimal("10000"), 3L);
        BigDecimal taken = layer.consume(new BigDecimal("100"));
        assertThat(taken).isEqualByComparingTo("20");
        assertThat(layer.getRemainingQty()).isEqualByComparingTo("0");
        assertThat(layer.isExhausted()).isTrue();
    }

    @Test
    public void fromInbound_correctValues() {
        Long mvId = 4L;
        CostLayer layer = CostLayer.fromInbound(1L, 1L, new BigDecimal("50"), new BigDecimal("10000"), mvId);
        assertThat(layer.getProductId()).isEqualTo(1L);
        assertThat(layer.getCostBasis()).isEqualTo("NORMAL");
        assertThat(layer.getInitialQty()).isEqualByComparingTo("50");
        assertThat(layer.getRemainingQty()).isEqualByComparingTo("50");
    }

    @Test
    public void fromReturn_correctValues() {
        Long mvId = 5L;
        CostLayer layer = CostLayer.fromReturn(1L, 1L, new BigDecimal("10"), new BigDecimal("8000"), mvId);
        assertThat(layer.getProductId()).isEqualTo(1L);
        assertThat(layer.getCostBasis()).isEqualTo("RETURN");
        assertThat(layer.getUnitCost()).isEqualByComparingTo("8000");
    }
}
