package com.store.erp.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderDto {
    private UUID id;
    private String salesOrderCode;
    private UUID customerId;
    private UUID quotationId;
    private String status;
    private String expectedDeliveryDate;
    private String remarks;
    private BigDecimal totalAmount;
    private List<SalesOrderItemDto> items;
}

