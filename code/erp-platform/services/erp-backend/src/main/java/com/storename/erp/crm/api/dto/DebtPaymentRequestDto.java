package com.storename.erp.crm.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Yêu cầu thu nợ độc lập: khách trả tiền cho khoản nợ đã có, không gắn với hóa đơn mới. */
@Data
public class DebtPaymentRequestDto {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Amount has too many digits")
    private BigDecimal amount;

    @Size(max = 500, message = "Note is too long")
    private String note;
}
