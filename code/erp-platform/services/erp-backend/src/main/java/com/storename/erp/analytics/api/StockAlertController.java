package com.storename.erp.analytics.api;

import com.storename.erp.analytics.api.dto.StockAlertSummaryDto;
import com.storename.erp.analytics.application.StockAlertService;
import com.storename.erp.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'HQ' or '${instance.role:ALL}' == 'ALL'")
@Slf4j
public class StockAlertController {

    private final StockAlertService stockAlertService;

    @GetMapping("/stock-alerts")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<StockAlertSummaryDto>> getStockAlerts(
            @RequestParam(required = false) Long branchId) {
        StockAlertSummaryDto summary = stockAlertService.getStockAlerts(branchId);
        return ResponseEntity.ok(ApiResponse.success(summary, "Stock alerts retrieved successfully"));
    }
}
