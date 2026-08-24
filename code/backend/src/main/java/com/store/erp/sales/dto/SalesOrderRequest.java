package com.store.erp.sales.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class SalesOrderRequest {
    private UUID customerId;
    private UUID quotationId;
    private String remarks;
    private LocalDate expectedDeliveryDate;
    private List<SalesOrderItemRequest> items;
}

