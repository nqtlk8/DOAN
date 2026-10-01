package com.storename.erp.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Lớp nền cho test chạy trên PostgreSQL thật (profile {@code postgres-it}, Flyway {@code db/migration}).
 *
 * <p>Container khởi động MỘT lần cho cả JVM (singleton pattern) và không gắn {@code @Container}.
 * Lý do: Spring cache ApplicationContext giữa các lớp test; nếu container bị dừng sau mỗi lớp
 * (hành vi của {@code @Testcontainers + @Container static}), context cache vẫn trỏ tới cổng của container
 * đã dừng và các lớp test sau lỗi "Failed to obtain JDBC Connection". Container được Ryuk dọn khi JVM kết thúc.</p>
 */
public abstract class PostgresIntegrationTest {

    protected static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("testcontainers.jdbc.url", postgres::getJdbcUrl);
        registry.add("testcontainers.jdbc.username", postgres::getUsername);
        registry.add("testcontainers.jdbc.password", postgres::getPassword);
    }
}
