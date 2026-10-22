package com.storename.erp.inventory.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class InboundReceiptCreateDto {
    
    private String receiptCode;
    
    private String note;
    
    private java.util.UUID supplierId;
    
    @NotEmpty(message = "Receipt must have at least one line")
    @Valid
    private List<InboundReceiptLineDto> lines;

    @Data
    public static class InboundReceiptLineDto {
        
        @NotNull(message = "Product ID is required")
        private Long productId;
        
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        @jakarta.validation.constraints.Digits(integer = 15, fraction = 4, message = "quantity allows at most 4 decimal places")
        private BigDecimal quantity;
        
        @NotNull(message = "Unit cost is required")
        @DecimalMin(value = "0.0", message = "Unit cost must not be negative")
        @jakarta.validation.constraints.Digits(integer = 15, fraction = 4, message = "unitCost allows at most 4 decimal places")
        private BigDecimal unitCost;
        
        @NotBlank(message = "Unit of measure is required")
        private String unitOfMeasure;
    }
}

