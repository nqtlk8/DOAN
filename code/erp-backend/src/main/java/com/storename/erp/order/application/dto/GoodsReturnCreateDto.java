package com.storename.erp.order.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class GoodsReturnCreateDto {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotBlank(message = "Return Code is required")
    private String returnCode;

    private UUID invoiceId; // nullable, for linking to original invoice

    private String reason;
    private String note;

    @NotEmpty(message = "Return must have at least one line")
    @Valid
    private List<LineDto> lines;

    @Data
    public static class LineDto {
        @NotNull(message = "Product ID is required")
        private UUID productId;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private BigDecimal quantity;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", message = "Unit price must not be negative")
        private BigDecimal unitPrice;

        @NotBlank(message = "Unit of measure is required")
        private String unitOfMeasure;
    }
}
