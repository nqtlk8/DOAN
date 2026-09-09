package com.storename.erp.analytics.api;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.analytics.api.dto.DashboardMetricsDto;
import com.storename.erp.analytics.application.DashboardService;
import com.storename.erp.analytics.application.ReportExportService;
import com.storename.erp.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReportExportService reportExportService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardMetricsDto>> getDashboardMetrics(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "20200101") Integer startDateKey,
            @RequestParam(defaultValue = "20301231") Integer endDateKey) {
        
        DashboardMetricsDto metrics = dashboardService.getDashboardMetrics(branchId, startDateKey, endDateKey);
        return ResponseEntity.ok(ApiResponse.success(metrics, "Dashboard metrics retrieved successfully"));
    }
    
    @GetMapping("/export/excel")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<byte[]> exportDashboardExcel(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "20200101") Integer startDateKey,
            @RequestParam(defaultValue = "20301231") Integer endDateKey) {
            
        byte[] excelBytes = reportExportService.exportDashboardToExcel(branchId, startDateKey, endDateKey);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "dashboard_report.xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
}
