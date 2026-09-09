package com.storename.erp.inventory.application.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class StockOnHandDto {
    private UUID id;
    private Long productId;
    private Long branchId;
    private BigDecimal quantity;
}
