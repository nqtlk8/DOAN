package com.storename.erp.order.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesInvoiceCreateResponseDto {
    private UUID id;
    private String invoiceCode;
}
