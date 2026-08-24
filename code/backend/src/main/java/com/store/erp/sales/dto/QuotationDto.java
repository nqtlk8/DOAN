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
public class QuotationDto {
    private UUID id;
    private String quotationCode;
    private UUID customerId;
    private String validUntil;
    private String status;
    private String remarks;
    private BigDecimal totalAmount;
    private List<QuotationItemDto> items;
}

