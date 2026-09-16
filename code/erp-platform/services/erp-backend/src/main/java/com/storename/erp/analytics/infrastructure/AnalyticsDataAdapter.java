package com.storename.erp.analytics.infrastructure;

import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class AnalyticsDataAdapter implements AnalyticsDataPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public BigDecimal getTotalReceivableDebt(Long branchId) {
        if (branchId == null) {
            String sql = "SELECT COALESCE(SUM(remaining_debt), 0) FROM sales_invoice WHERE status = 'CONFIRMED' AND is_deleted = false";
            return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), BigDecimal.class);
        } else {
            String sql = "SELECT COALESCE(SUM(total_debt), 0) FROM receivable_debt WHERE branch_id = :branchId";
            return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("branchId", branchId), BigDecimal.class);
        }
    }

    @Override
    public BigDecimal getCurrentStockQuantity(Long productId, Long branchId) {
        if (branchId == null) {
            String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock_movement WHERE product_id = :productId";
            return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("productId", productId), BigDecimal.class);
        } else {
            String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock_on_hand WHERE product_id = :productId AND branch_id = :branchId";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("productId", productId);
            params.addValue("branchId", branchId);
            return jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        }
    }

    @Override
    public java.util.List<com.storename.erp.analytics.api.dto.ProductPerformanceDto> getTopSellingProducts(Long branchId, Integer startDateKey, Integer endDateKey) {
        String sql = """
            SELECT l.product_id, l.product_name, SUM(l.quantity) as quantity_sold, SUM(l.quantity * l.unit_price) as revenue 
            FROM sales_invoice_line l 
            JOIN sales_invoice i ON l.invoice_id = i.id 
            WHERE (:branchId IS NULL OR i.branch_id = :branchId) AND i.confirmed_at IS NOT NULL 
              AND i.confirmed_at >= to_date(:startDateKey::text, 'YYYYMMDD') 
              AND i.confirmed_at < to_date(:endDateKey::text, 'YYYYMMDD') + INTERVAL '1' DAY
              AND l.is_deleted = false AND i.is_deleted = false
            GROUP BY l.product_id, l.product_name 
            ORDER BY quantity_sold DESC LIMIT 10
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("branchId", branchId);
        params.addValue("startDateKey", startDateKey);
        params.addValue("endDateKey", endDateKey);
        
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            com.storename.erp.analytics.api.dto.ProductPerformanceDto dto = new com.storename.erp.analytics.api.dto.ProductPerformanceDto();
            dto.setProductId(rs.getLong("product_id"));
            dto.setProductName(rs.getString("product_name"));
            dto.setQuantitySold(rs.getBigDecimal("quantity_sold"));
            dto.setRevenue(rs.getBigDecimal("revenue"));
            return dto;
        });
    }

    @Override
    public BigDecimal getTotalRevenue(Long branchId, Integer startDateKey, Integer endDateKey) {
        String sql = """
            SELECT COALESCE(SUM(l.quantity * l.unit_price), 0) 
            FROM sales_invoice_line l 
            JOIN sales_invoice i ON l.invoice_id = i.id 
            WHERE (:branchId IS NULL OR i.branch_id = :branchId) AND i.confirmed_at IS NOT NULL 
              AND i.confirmed_at >= to_date(:startDateKey::text, 'YYYYMMDD') 
              AND i.confirmed_at < to_date(:endDateKey::text, 'YYYYMMDD') + INTERVAL '1' DAY
              AND l.is_deleted = false AND i.is_deleted = false
        """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("branchId", branchId);
        params.addValue("startDateKey", startDateKey);
        params.addValue("endDateKey", endDateKey);
        return jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
    }

    @Override
    public BigDecimal getTotalGrossProfit(Long branchId, Integer startDateKey, Integer endDateKey) {
        String sql = """
            SELECT COALESCE(SUM(l.quantity * l.unit_price) - SUM(l.quantity * COALESCE(l.unit_cost, 0)), 0) 
            FROM sales_invoice_line l 
            JOIN sales_invoice i ON l.invoice_id = i.id 
            WHERE (:branchId IS NULL OR i.branch_id = :branchId) AND i.confirmed_at IS NOT NULL 
              AND i.confirmed_at >= to_date(:startDateKey::text, 'YYYYMMDD') 
              AND i.confirmed_at < to_date(:endDateKey::text, 'YYYYMMDD') + INTERVAL '1' DAY
              AND l.is_deleted = false AND i.is_deleted = false
        """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("branchId", branchId);
        params.addValue("startDateKey", startDateKey);
        params.addValue("endDateKey", endDateKey);
        return jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
    }

    @Override
    public BigDecimal getTotalCogs(Long branchId, Integer startDateKey, Integer endDateKey) {
        String sql = """
            SELECT COALESCE(SUM(l.quantity * COALESCE(l.unit_cost, 0)), 0) 
            FROM sales_invoice_line l 
            JOIN sales_invoice i ON l.invoice_id = i.id 
            WHERE (:branchId IS NULL OR i.branch_id = :branchId) AND i.confirmed_at IS NOT NULL 
              AND i.confirmed_at >= to_date(:startDateKey::text, 'YYYYMMDD') 
              AND i.confirmed_at < to_date(:endDateKey::text, 'YYYYMMDD') + INTERVAL '1' DAY
              AND l.is_deleted = false AND i.is_deleted = false
        """;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("branchId", branchId);
        params.addValue("startDateKey", startDateKey);
        params.addValue("endDateKey", endDateKey);
        return jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
    }
}
