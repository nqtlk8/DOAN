package com.storename.erp.crm.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashReceiptCreateResponseDto {
    private UUID id;
    private String receiptCode;
}
