package com.storename.erp.order.application.dto;

import com.storename.erp.order.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SalesInvoiceCreateDto {
    @NotBlank(message = "Invoice code is required")
    private String invoiceCode;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String note;

    @NotEmpty(message = "Invoice lines cannot be empty")
    @Valid
    private List<SalesInvoiceLineDto> lines;
}
