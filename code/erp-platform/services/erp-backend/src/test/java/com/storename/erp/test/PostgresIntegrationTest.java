package com.storename.erp.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class PostgresIntegrationTest {

    // Singleton container to be shared across all tests extending this class
    @Container
    protected static final PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("testcontainers.jdbc.url", postgres::getJdbcUrl);
        registry.add("testcontainers.jdbc.username", postgres::getUsername);
        registry.add("testcontainers.jdbc.password", postgres::getPassword);
    }
}
