package com.storename.erp.test;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("postgres-it")
// Disable auto flyway and hibernate ddl to control manually
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
public class FlywayUpgradePostgresIT extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testUpgradeFromV17ToV18() {
        // Step 1: Migrate up to V17 only
        Flyway flywayV17 = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("17")
                .load();
        flywayV17.migrate();

        // Step 2: Insert dummy data using V17 schema (before harden)
        // V17 schema may not have idempotency_key or NOT NULL constraints that V18 adds
        UUID customerId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        
        jdbcTemplate.update("INSERT INTO branch (id, name, code) VALUES (?, 'Branch 17', 'B17')", branchId);
        jdbcTemplate.update("INSERT INTO customer (id, full_name, phone) VALUES (?, 'Customer 17', '0999')", customerId);
        
        jdbcTemplate.update(
                "INSERT INTO receivable_debt (customer_id, branch_id, total_debt) VALUES (?, ?, 10000000)",
                customerId, branchId
        );

        // Step 3: Run migration V18 (Harden)
        Flyway flywayV18 = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("18") // or simply don't set target to go to latest
                .load();
        flywayV18.migrate();

        // Step 4: Verify data survived and conforms to V18 schema
        BigDecimal debt = jdbcTemplate.queryForObject(
                "SELECT total_debt FROM receivable_debt WHERE customer_id = ? AND branch_id = ?",
                BigDecimal.class,
                customerId, branchId
        );

        // Ensures data wasn't truncated/dropped
        assertNotNull(debt);
        assertEquals(0, new BigDecimal("10000000").compareTo(debt));
        
        // V18 might have backfilled balance_before or idempotency keys, which we could verify here if they existed on debt/movement
    }
}
