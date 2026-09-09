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
}
