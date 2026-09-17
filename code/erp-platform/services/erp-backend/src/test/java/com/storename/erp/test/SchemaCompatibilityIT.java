package com.storename.erp.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("postgres-it")
public class SchemaCompatibilityIT extends PostgresIntegrationTest {

    @Test
    void testHibernateSchemaValidation() {
        // Since spring.jpa.hibernate.ddl-auto=validate is set in application-postgres-it.yml
        // If this test runs and the context loads successfully, it strictly proves that:
        // 1. The PostgreSQL schema created by Flyway matches EXACTLY the JPA Entity definitions.
        // 2. No columns are missing.
        // 3. Data types are fully compatible.
        assertTrue(postgres.isRunning(), "Spring Context loaded, meaning schema validation passed.");
    }
}
