package com.storename.erp.inventory.unit;

import com.storename.erp.inventory.domain.StockOnHand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StockOnHandUnitTest {

    @Test
    public void testDoubleCheckQuantity() {
        StockOnHand stock = new StockOnHand(1L, 1L);
        assertEquals(0, stock.getQuantity().compareTo(BigDecimal.ZERO));

        // Nhập: [10, 5, 8, 3, 7] -> tổng 33
        stock.increase(new BigDecimal("10"), "Nhập");
        stock.increase(new BigDecimal("5"), "Nhập");
        stock.increase(new BigDecimal("8"), "Nhập");
        stock.increase(new BigDecimal("3"), "Nhập");
        stock.increase(new BigDecimal("7"), "Nhập");

        assertEquals(0, stock.getQuantity().compareTo(new BigDecimal("33")));

        // Xuất: [6, 4, 2, 9] -> tổng 21
        stock.decrease(new BigDecimal("6"), "Xuất");
        stock.decrease(new BigDecimal("4"), "Xuất");
        stock.decrease(new BigDecimal("2"), "Xuất");
        stock.decrease(new BigDecimal("9"), "Xuất");

        // Expected: 33 - 21 = 12
        assertEquals(0, stock.getQuantity().compareTo(new BigDecimal("12")));
    }
}
