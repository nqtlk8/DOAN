package com.storename.erp.analytics.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEtlJob {
    
    // LƯU Ý: Job này hiện là khung lịch chạy định kỳ, CHƯA hiện thực logic ETL vào
    // FactSales/FactStockMovement. Dashboard hiện tại đọc trực tiếp từ bảng nghiệp vụ
    // (sales_invoice, sales_invoice_line) qua AnalyticsDataAdapter, KHÔNG phụ thuộc job này.
    // Việc hiện thực ETL đầy đủ là hướng phát triển tiếp theo (xem docs/FOUND_ISSUES.md).
    // In a real system, this job would extract from SalesInvoice & StockMovement 
    // and load into FactSales & FactStockMovement.
    
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void runEtlSync() {
        log.info("[ETL] Starting Analytics Data Mart synchronization... (SCAFFOLD ONLY - NOT HYDRATING DASHBOARD)");
        // Implement extraction logic here:
        // 1. Fetch new confirmed sales_invoice records since last sync
        // 2. Transform into FactSales
        // 3. Save to FactSalesRepository
        
        // 4. Fetch new stock movements (Receipts)
        // 5. Transform into FactStockMovement
        // 6. Save to FactStockMovementRepository
        log.info("[ETL] Finished Analytics Data Mart synchronization.");
    }
}
