package com.storename.erp.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("postgres-it")
public class FlywayPostgresIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testFlywayMigrationSuccessAndHibernateValidation() {
        // If the context starts up successfully, it means:
        // 1. Flyway successfully ran all migrations
        // 2. Hibernate successfully validated the schema (ddl-auto=validate)
        assertTrue(postgres.isRunning());
    }

    @Test
    @SuppressWarnings("SqlResolve")
    void testFlywaySchemaHistory() {
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank"
        );

        assertFalse(history.isEmpty(), "Flyway history should not be empty");

        for (Map<String, Object> record : history) {
            Boolean success = (Boolean) record.get("success");
            assertTrue(success, "Migration " + record.get("version") + " (" + record.get("description") + ") should be successful");
        }
    }

    @Test
    void testRequiredTablesExist() {
        String[] requiredTables = {
                "receivable_debt",
                "receivable_debt_movement",
                "sales_invoice",
                "stock_movement",
                "stock_on_hand",
                "customer",
                "product",
                "branch"
        };

        for (String table : requiredTables) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                    Integer.class,
                    table
            );
            assertTrue(count != null && count > 0, "Table " + table + " should exist in public schema");
        }
    }
}
