package com.storename.erp.crm.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class OpeningBalanceRequestDto {
    private UUID customerId;
    private BigDecimal amount;
    private String note;
}
