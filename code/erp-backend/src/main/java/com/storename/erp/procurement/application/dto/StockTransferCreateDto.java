package com.storename.erp.procurement.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class StockTransferCreateDto {

    @NotNull(message = "Destination branch ID is required")
    private UUID toBranchId;

    @NotBlank(message = "Transfer Code is required")
    private String transferCode;

    private String note;

    @NotEmpty(message = "Transfer must have at least one line")
    @Valid
    private List<LineDto> lines;

    @Data
    public static class LineDto {
        @NotNull(message = "Product ID is required")
        private UUID productId;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private BigDecimal quantity;

        @NotBlank(message = "Unit of measure is required")
        private String unitOfMeasure;
    }
}
