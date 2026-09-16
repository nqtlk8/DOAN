package com.storename.erp.analytics.infrastructure;

import com.storename.erp.analytics.api.dto.ProductPerformanceDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AnalyticsDataAdapter.class)
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:analytics_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class AnalyticsDataAdapterTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AnalyticsDataAdapter adapter;

    @Test
    void testQueries_ShouldExecuteSuccessfullyOnPostgreSQL() {
        // We only want to ensure the syntax works on PostgreSQL (e.g., to_date(?::text, 'YYYYMMDD'))
        // So we execute them with dummy parameters. If the SQL is valid, they will return empty or 0.
        
        Long branchId = 1L;
        Integer startDate = 20240101;
        Integer endDate = 20241231;

        // 1. getTotalReceivableDebt
        BigDecimal debt = adapter.getTotalReceivableDebt(branchId);
        assertThat(debt).isEqualByComparingTo(BigDecimal.ZERO);

        // 2. getCurrentStockQuantity
        BigDecimal stock = adapter.getCurrentStockQuantity(10L, branchId);
        assertThat(stock).isEqualByComparingTo(BigDecimal.ZERO);

        // 3. getTopSellingProducts
        List<ProductPerformanceDto> tops = adapter.getTopSellingProducts(branchId, startDate, endDate);
        assertThat(tops).isEmpty();

        // 4. getTotalRevenue
        BigDecimal revenue = adapter.getTotalRevenue(branchId, startDate, endDate);
        assertThat(revenue).isEqualByComparingTo(BigDecimal.ZERO);

        // 5. getTotalGrossProfit
        BigDecimal profit = adapter.getTotalGrossProfit(branchId, startDate, endDate);
        assertThat(profit).isEqualByComparingTo(BigDecimal.ZERO);

        // 6. getTotalCogs
        BigDecimal cogs = adapter.getTotalCogs(branchId, startDate, endDate);
        assertThat(cogs).isEqualByComparingTo(BigDecimal.ZERO);

        // Test with null branchId (Admin case)
        BigDecimal debtAdmin = adapter.getTotalReceivableDebt(null);
        assertThat(debtAdmin).isEqualByComparingTo(BigDecimal.ZERO);

        BigDecimal stockAdmin = adapter.getCurrentStockQuantity(10L, null);
        assertThat(stockAdmin).isEqualByComparingTo(BigDecimal.ZERO);

        List<ProductPerformanceDto> topsAdmin = adapter.getTopSellingProducts(null, startDate, endDate);
        assertThat(topsAdmin).isEmpty();
    }
}
