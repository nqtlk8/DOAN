package com.storename.erp.inventory.unit;

import com.storename.erp.inventory.domain.StockOnHand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import com.storename.erp.inventory.domain.StockInsufficientException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class StockOnHandUnitTest {

    @Test
    public void increase_addsToQuantity() {
        StockOnHand stock = new StockOnHand(1L, 1L);
        stock.increase(new BigDecimal("10"), "init");
        stock.increase(new BigDecimal("5"), "increase");
        assertThat(stock.getQuantity()).isEqualByComparingTo("15");
    }

    @Test
    public void increase_zeroOrNegative_throws() {
        StockOnHand stock = new StockOnHand(1L, 1L);
        assertThatThrownBy(() -> stock.increase(BigDecimal.ZERO, "zero"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.increase(new BigDecimal("-1"), "negative"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void decrease_subtractsQuantity() {
        StockOnHand stock = new StockOnHand(1L, 1L);
        stock.increase(new BigDecimal("10"), "init");
        stock.decrease(new BigDecimal("3"), "decrease");
        assertThat(stock.getQuantity()).isEqualByComparingTo("7");
    }

    @Test
    public void decrease_insufficientStock_throws() {
        StockOnHand stock = new StockOnHand(1L, 1L);
        stock.increase(new BigDecimal("5"), "init");
        assertThatThrownBy(() -> stock.decrease(new BigDecimal("10"), "decrease"))
                .isInstanceOf(StockInsufficientException.class);
    }

    @Test
    public void decreaseAllowNegative_goesBelow() {
        StockOnHand stock = new StockOnHand(1L, 1L);
        stock.increase(new BigDecimal("5"), "init");
        stock.decreaseAllowNegative(new BigDecimal("10"), "decrease");
        assertThat(stock.getQuantity()).isEqualByComparingTo("-5");
    }

    @Test
    public void decrease_zeroOrNegative_throws() {
        StockOnHand stock = new StockOnHand(1L, 1L);
        assertThatThrownBy(() -> stock.decrease(BigDecimal.ZERO, "zero"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
