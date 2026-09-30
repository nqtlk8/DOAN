package com.storename.erp.analytics.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertSummaryDto {
    private int negativeCount;
    private int lowStockCount;
    private OffsetDateTime generatedAt;
    private List<StockAlertDto> alerts;
}
