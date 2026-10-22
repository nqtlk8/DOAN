package com.storename.erp.crm.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CashReceiptCreateDto {

    @NotNull(message = "Vui lòng chọn khách hàng")
    private UUID customerId;

    @NotNull(message = "Vui lòng nhập số tiền thu")
    @DecimalMin(value = "0.0", inclusive = false, message = "Số tiền thu phải lớn hơn 0")
    @Digits(integer = 15, fraction = 4, message = "Số tiền thu tối đa 4 chữ số thập phân")
    private BigDecimal amount;

    /** Ngày thu; bỏ trống = hôm nay. Không được sau hôm nay. */
    private LocalDate receiptDate;

    @Size(max = 1000, message = "Diễn giải tối đa 1000 ký tự")
    private String note;
}
