package com.storename.erp.analytics.application;

import com.storename.erp.analytics.api.dto.DashboardMetricsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    private final DashboardService dashboardService;

    public byte[] exportDashboardToExcel(Long branchId, Integer startDateKey, Integer endDateKey) {
        DashboardMetricsDto metrics = dashboardService.getDashboardMetrics(branchId, startDateKey, endDateKey);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Dashboard Report");
            
            // Create styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Metric", "Value"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Data Rows
            int rowNum = 1;
            
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Total Revenue");
            row.createCell(1).setCellValue(metrics.getTotalRevenue() != null ? metrics.getTotalRevenue().doubleValue() : 0.0);
            
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Gross Profit");
            row.createCell(1).setCellValue(metrics.getGrossProfit() != null ? metrics.getGrossProfit().doubleValue() : 0.0);
            
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Inventory Turnover");
            row.createCell(1).setCellValue(metrics.getInventoryTurnoverRatio() != null ? metrics.getInventoryTurnoverRatio().doubleValue() : 0.0);
            
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Total Overdue Debt");
            row.createCell(1).setCellValue(metrics.getTotalOverdueDebt() != null ? metrics.getTotalOverdueDebt().doubleValue() : 0.0);
            
            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
            
        } catch (IOException e) {
            log.error("Error generating Excel report", e);
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }
}
