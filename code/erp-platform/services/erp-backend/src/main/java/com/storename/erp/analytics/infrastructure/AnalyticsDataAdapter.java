package com.storename.erp.analytics.infrastructure;

import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class AnalyticsDataAdapter implements AnalyticsDataPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public BigDecimal getTotalReceivableDebt(Long branchId) {
        String sql = "SELECT COALESCE(SUM(remaining_amount), 0) FROM receivable_debt WHERE branch_id = ?";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, branchId);
    }

    @Override
    public BigDecimal getCurrentStockQuantity(Long productId, Long branchId) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock_on_hand WHERE product_id = ? AND branch_id = ?";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, productId, branchId);
    }



    @Override
    public java.util.List<com.storename.erp.analytics.api.dto.ProductPerformanceDto> getTopSellingProducts(Long branchId, Integer startDateKey, Integer endDateKey) {
        String sql = """
            SELECT l.product_id, l.product_name, SUM(l.quantity) as quantity_sold, SUM(l.quantity * l.unit_price) as revenue 
            FROM sales_invoice_line l 
            JOIN sales_invoice i ON l.invoice_id = i.id 
            WHERE i.branch_id = ? AND i.confirmed_at IS NOT NULL 
              AND i.confirmed_at >= to_date(?::text, 'YYYYMMDD') 
              AND i.confirmed_at < to_date(?::text, 'YYYYMMDD') + interval '1 day'
              AND l.is_deleted = false AND i.is_deleted = false
            GROUP BY l.product_id, l.product_name 
            ORDER BY quantity_sold DESC LIMIT 10
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            com.storename.erp.analytics.api.dto.ProductPerformanceDto dto = new com.storename.erp.analytics.api.dto.ProductPerformanceDto();
            dto.setProductId(rs.getLong("product_id"));
            dto.setProductName(rs.getString("product_name"));
            dto.setQuantitySold(rs.getBigDecimal("quantity_sold"));
            dto.setRevenue(rs.getBigDecimal("revenue"));
            return dto;
        }, branchId, startDateKey, endDateKey);
    }

    @Override
    public BigDecimal getTotalRevenue(Long branchId, Integer startDateKey, Integer endDateKey) {
        String sql = """
            SELECT COALESCE(SUM(l.quantity * l.unit_price), 0) 
            FROM sales_invoice_line l 
            JOIN sales_invoice i ON l.invoice_id = i.id 
            WHERE i.branch_id = ? AND i.confirmed_at IS NOT NULL 
              AND i.confirmed_at >= to_date(?::text, 'YYYYMMDD') 
              AND i.confirmed_at < to_date(?::text, 'YYYYMMDD') + interval '1 day'
              AND l.is_deleted = false AND i.is_deleted = false
        """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, branchId, startDateKey, endDateKey);
    }

    @Override
    public BigDecimal getTotalGrossProfit(Long branchId, Integer startDateKey, Integer endDateKey) {
        String sql = """
            SELECT COALESCE(SUM(l.quantity * l.unit_price) - SUM(l.quantity * COALESCE(l.unit_cost, 0)), 0) 
            FROM sales_invoice_line l 
            JOIN sales_invoice i ON l.invoice_id = i.id 
            WHERE i.branch_id = ? AND i.confirmed_at IS NOT NULL 
              AND i.confirmed_at >= to_date(?::text, 'YYYYMMDD') 
              AND i.confirmed_at < to_date(?::text, 'YYYYMMDD') + interval '1 day'
              AND l.is_deleted = false AND i.is_deleted = false
        """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, branchId, startDateKey, endDateKey);
    }

    @Override
    public BigDecimal getTotalCogs(Long branchId, Integer startDateKey, Integer endDateKey) {
        String sql = """
            SELECT COALESCE(SUM(l.quantity * COALESCE(l.unit_cost, 0)), 0) 
            FROM sales_invoice_line l 
            JOIN sales_invoice i ON l.invoice_id = i.id 
            WHERE i.branch_id = ? AND i.confirmed_at IS NOT NULL 
              AND i.confirmed_at >= to_date(?::text, 'YYYYMMDD') 
              AND i.confirmed_at < to_date(?::text, 'YYYYMMDD') + interval '1 day'
              AND l.is_deleted = false AND i.is_deleted = false
        """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, branchId, startDateKey, endDateKey);
    }
}
