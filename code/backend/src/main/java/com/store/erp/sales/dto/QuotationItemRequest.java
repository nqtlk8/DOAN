package com.store.erp.sales.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class QuotationItemRequest {
    private UUID productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
}

