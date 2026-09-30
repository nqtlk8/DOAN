package com.storename.erp.analytics.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockLevel {
    private Long productId;
    private Long branchId;
    private BigDecimal quantity;
}
