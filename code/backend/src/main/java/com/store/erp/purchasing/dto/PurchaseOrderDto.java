package com.store.erp.purchasing.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PurchaseOrderDto {
    private UUID id;
    private String orderNumber;
    private UUID distributorId;
    private LocalDate orderDate;
    private BigDecimal totalAmount;
    private String status;
    private String notes;
    private List<PurchaseOrderItemDto> items;
}

