package com.store.erp.sales.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class QuotationRequest {
    private UUID customerId;
    private LocalDate validUntil;
    private String remarks;
    private List<QuotationItemRequest> items;
}

