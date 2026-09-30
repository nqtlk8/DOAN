package com.storename.erp.analytics.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlowMovingProductDto {
    private Long productId;
    private String productCode;
    private String productName;
    private BigDecimal currentStock;
}
