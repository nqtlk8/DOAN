package com.store.erp.purchasing.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PurchaseOrderItemDto {
    private UUID id;
    private UUID productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}

