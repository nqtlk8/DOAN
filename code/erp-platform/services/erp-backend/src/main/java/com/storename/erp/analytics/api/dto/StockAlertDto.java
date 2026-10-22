package com.storename.erp.analytics.api.dto;

import com.storename.erp.analytics.domain.StockAlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertDto {
    private Long productId;
    private String productCode;
    private String productName;
    private Long branchId;
    private String branchName;
    private BigDecimal currentQuantity;
    private BigDecimal minQuantityThreshold;
    private StockAlertType alertType;
}
