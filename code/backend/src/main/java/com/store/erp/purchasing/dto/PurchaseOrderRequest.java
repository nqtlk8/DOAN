package com.store.erp.purchasing.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

@Data
public class PurchaseOrderRequest {
    @NotNull(message = "Distributor ID is required")
    private UUID distributorId;
    
    @NotNull(message = "Order date is required")
    private LocalDate orderDate;
    
    private String notes;
    
    @NotEmpty(message = "Items list cannot be empty")
    private List<PurchaseOrderItemRequest> items;
}

