package com.storename.erp.analytics.infrastructure;

import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import com.storename.erp.analytics.api.dto.ProductPerformanceDto;
import com.storename.erp.analytics.domain.SalesTotals;
import com.storename.erp.analytics.domain.StockLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Adapter đọc dữ liệu báo cáo trực tiếp từ bảng nghiệp vụ (không qua ETL/fact tables).
 *
 * <p>Nguồn dữ liệu: doanh thu/giá vốn từ {@code sales_invoice(_line)} đã CONFIRMED; tồn kho từ
 * {@code stock_movement} (append-only, {@code SUM(quantity)} = tồn); giá trị tồn từ {@code cost_layer};
 * công nợ từ {@code receivable_debt} (replicate từ Branch lên HQ có row filter — ADR-13).</p>
 *
 * <p>Quy ước: tham số {@code branchId} luôn tạo qua {@link AnalyticsSqlParams} (null có kiểu); lọc thời gian
 * theo khoảng {@code [fromTs, toTs)} để dùng được index trên {@code confirmed_at}.</p>
 */
@Repository
@RequiredArgsConstructor
public class AnalyticsDataAdapter implements AnalyticsDataPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public SalesTotals getSalesTotals(Long branchId, LocalDateTime fromTs, LocalDateTime toTs) {
        String sql = """
            SELECT 
                COALESCE(SUM(l.line_total), 0) as revenue,
                COALESCE(SUM(l.quantity * COALESCE(l.unit_cost, 0)), 0) as cogs
            FROM sales_invoice_line l 
            JOIN sales_invoice i ON l.invoice_id = i.id 
            WHERE (:branchId IS NULL OR i.branch_id = :branchId) 
              AND i.confirmed_at >= :fromTs 
              AND i.confirmed_at < :toTs
              AND i.status = 'CONFIRMED'
              AND l.is_deleted = false AND i.is_deleted = false
        """;
        MapSqlParameterSource params = AnalyticsSqlParams.withBranchId(branchId);
        params.addValue("fromTs", fromTs, Types.TIMESTAMP);
        params.addValue("toTs", toTs, Types.TIMESTAMP);
        
        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {
            return new SalesTotals(rs.getBigDecimal("revenue"), rs.getBigDecimal("cogs"));
        });
    }

    @Override
    public List<ProductPerformanceDto> getTopProductsByRevenue(Long branchId, LocalDateTime fromTs, LocalDateTime toTs, int limit) {
        String sql = """
            SELECT l.product_id, l.product_name, SUM(l.quantity) as quantity_sold, SUM(l.line_total) as revenue 
            FROM sales_invoice_line l 
            JOIN sales_invoice i ON l.invoice_id = i.id 
            WHERE (:branchId IS NULL OR i.branch_id = :branchId) 
              AND i.confirmed_at >= :fromTs 
              AND i.confirmed_at < :toTs
              AND i.status = 'CONFIRMED'
              AND l.is_deleted = false AND i.is_deleted = false
            GROUP BY l.product_id, l.product_name 
            ORDER BY revenue DESC LIMIT :limit
        """;
        
        MapSqlParameterSource params = AnalyticsSqlParams.withBranchId(branchId);
        params.addValue("fromTs", fromTs, Types.TIMESTAMP);
        params.addValue("toTs", toTs, Types.TIMESTAMP);
        params.addValue("limit", limit, Types.INTEGER);
        
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            ProductPerformanceDto dto = new ProductPerformanceDto();
            dto.setProductId(rs.getLong("product_id"));
            dto.setProductName(rs.getString("product_name"));
            dto.setQuantitySold(rs.getBigDecimal("quantity_sold"));
            dto.setRevenue(rs.getBigDecimal("revenue"));
            return dto;
        });
    }

    @Override
    public BigDecimal getInventoryValue(Long branchId) {
        String sql = """
            SELECT COALESCE(SUM(remaining_qty * unit_cost), 0)
            FROM cost_layer
            WHERE (:branchId IS NULL OR branch_id = :branchId)
        """;
        MapSqlParameterSource params = AnalyticsSqlParams.withBranchId(branchId);
        return jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
    }

    @Override
    public BigDecimal getTotalReceivableDebt(Long branchId) {
        String sql = """
            SELECT COALESCE(SUM(GREATEST(total_debt, 0)), 0) 
            FROM receivable_debt 
            WHERE (:branchId IS NULL OR branch_id = :branchId)
              AND is_deleted = false
        """;
        MapSqlParameterSource params = AnalyticsSqlParams.withBranchId(branchId);
        return jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
    }

    @Override
    public List<StockLevel> getStockLevels(Long branchId) {
        String sql = """
            SELECT product_id, branch_id, SUM(quantity) as quantity
            FROM stock_movement 
            WHERE (:branchId IS NULL OR branch_id = :branchId)
            GROUP BY product_id, branch_id
        """;
        MapSqlParameterSource params = AnalyticsSqlParams.withBranchId(branchId);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            return new StockLevel(
                rs.getLong("product_id"),
                rs.getLong("branch_id"),
                rs.getBigDecimal("quantity")
            );
        });
    }

    @Override
    public List<Long> getProductIdsSoldInPeriod(Long branchId, LocalDateTime fromTs, LocalDateTime toTs) {
        String sql = """
            SELECT DISTINCT l.product_id
            FROM sales_invoice_line l 
            JOIN sales_invoice i ON l.invoice_id = i.id 
            WHERE (:branchId IS NULL OR i.branch_id = :branchId) 
              AND i.confirmed_at >= :fromTs 
              AND i.confirmed_at < :toTs
              AND i.status = 'CONFIRMED'
              AND l.is_deleted = false AND i.is_deleted = false
        """;
        MapSqlParameterSource params = AnalyticsSqlParams.withBranchId(branchId);
        params.addValue("fromTs", fromTs, Types.TIMESTAMP);
        params.addValue("toTs", toTs, Types.TIMESTAMP);
        return jdbcTemplate.queryForList(sql, params, Long.class);
    }
}
