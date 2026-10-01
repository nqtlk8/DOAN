package com.storename.erp.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("postgres-it")
@org.springframework.transaction.annotation.Transactional
public class ReceivableDebtLedgerPostgresIT extends PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testReceivableDebtMovementTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'receivable_debt_movement'",
                Integer.class
        );
        assertTrue(count != null && count > 0, "Table receivable_debt_movement should exist");
    }

    @Test
    void testReceivableDebtMovementColumns() {
        List<String> expectedColumns = List.of(
                "id", "customer_id", "branch_id", "movement_type", "amount",
                "balance_before", "balance_after", "ref_type", "ref_id",
                "idempotency_key", "created_at"
        );

        List<String> actualColumns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'receivable_debt_movement'",
                String.class
        );

        for (String expectedColumn : expectedColumns) {
            assertTrue(actualColumns.contains(expectedColumn), "Column " + expectedColumn + " is missing in receivable_debt_movement");
        }
    }

    /**
     * Ràng buộc uk_debt_movement_idempotency phải chặn ghi trùng cùng một nghiệp vụ ở mức DB.
     * receivable_debt_movement có FK sang customer (không có FK sang branch) nên chỉ cần chèn khách hàng.
     */
    @Test
    void testIdempotencyKeyUniqueConstraint() {
        String idempotencyKey = "KEY-A-" + UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        long branchId = 903L;
        String refId = UUID.randomUUID().toString();

        jdbcTemplate.update(
                "INSERT INTO customer (id, customer_code, name, phone, version, is_deleted) VALUES (?, ?, 'Test Customer', '0123', 0, false)",
                customerId, "IT-LEDGER-" + customerId.toString().substring(0, 8));

        String sql = "INSERT INTO receivable_debt_movement (id, customer_id, branch_id, movement_type, amount, balance_before, "
                + "balance_after, ref_type, ref_id, idempotency_key, created_at) "
                + "VALUES (?, ?, ?, 'PAYMENT', 100, 0, 100, 'INVOICE', ?, ?, now())";

        // Lần 1: thành công
        jdbcTemplate.update(sql, UUID.randomUUID(), customerId, branchId, refId, idempotencyKey);

        // Lần 2 cùng idempotency_key: phải bị DB từ chối
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(sql, UUID.randomUUID(), customerId, branchId, refId, idempotencyKey),
                "Expected unique constraint violation on idempotency_key");
    }
}
