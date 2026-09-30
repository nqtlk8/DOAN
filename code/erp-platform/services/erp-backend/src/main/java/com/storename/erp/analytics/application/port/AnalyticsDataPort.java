package com.storename.erp.analytics.application.port;

import com.storename.erp.analytics.api.dto.ProductPerformanceDto;
import com.storename.erp.analytics.domain.SalesTotals;
import com.storename.erp.analytics.domain.StockLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsDataPort {

    /**
     * Get revenue and COGS for a period.
     * Table: sales_invoice, sales_invoice_line
     */
    SalesTotals getSalesTotals(Long branchId, LocalDateTime fromTs, LocalDateTime toTs);

    /**
     * Get top products by revenue.
     * Table: sales_invoice, sales_invoice_line
     */
    List<ProductPerformanceDto> getTopProductsByRevenue(Long branchId, LocalDateTime fromTs, LocalDateTime toTs, int limit);

    /**
     * Get total inventory value based on cost layer.
     * Table: cost_layer
     */
    BigDecimal getInventoryValue(Long branchId);

    /**
     * Get total receivable debt for a branch.
     * Table: receivable_debt
     */
    BigDecimal getTotalReceivableDebt(Long branchId);

    /**
     * Get stock levels for alerts.
     * Table: stock_movement
     */
    List<StockLevel> getStockLevels(Long branchId);

    /**
     * Get product ids sold in period to find slow moving products.
     * Table: sales_invoice, sales_invoice_line
     */
    List<Long> getProductIdsSoldInPeriod(Long branchId, LocalDateTime fromTs, LocalDateTime toTs);
}
