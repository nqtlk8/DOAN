package com.store.erp.sales.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotationResponse {
    private UUID quotationId;
    private String quotationCode;
}

