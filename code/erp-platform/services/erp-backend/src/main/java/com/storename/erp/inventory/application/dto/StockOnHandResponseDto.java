package com.storename.erp.inventory.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class StockOnHandResponseDto {
    private UUID id;
    private Long productId;
    private String productCode;
    private String productName;
    private Long branchId;
    private String branchName;
    private BigDecimal quantity;
    private BigDecimal avgCost;
}
