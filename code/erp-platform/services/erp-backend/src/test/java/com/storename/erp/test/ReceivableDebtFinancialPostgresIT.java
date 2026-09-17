package com.storename.erp.test;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.domain.ReceivableDebtMovement;
import com.storename.erp.crm.domain.ReceivableDebtMovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("postgres-it")
@Transactional
public class ReceivableDebtFinancialPostgresIT extends PostgresIntegrationTest {

    @Autowired
    private ReceivableDebtService debtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID customerId;
    private final Long branchId = 1L;
    private final UUID adminUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        // Insert dummy customer directly to bypass JPA mandatory field checks in unit tests
        jdbcTemplate.update(
                "INSERT INTO customer (id, full_name, phone) VALUES (?, 'Fin Customer', '0999888777')",
                customerId
        );
        jdbcTemplate.update(
                "INSERT INTO branch (id, name, code) VALUES (?, 'Fin Branch', 'FB1')",
                branchId
        );
    }

    @Test
    void testFinancialConsistencyOverMultipleTransactions() {
        // opening = +5m
        debtService.setOpeningBalance(customerId, branchId, new BigDecimal("5000000"), adminUserId, "Opening");

        // invoice = +10m
        debtService.increaseDebt(customerId, branchId, new BigDecimal("10000000"),
                ReceivableDebtMovementType.INVOICE, "INVOICE", "INV-100", adminUserId, "Invoice created");

        // payment = -3m
        debtService.decreaseDebt(customerId, branchId, new BigDecimal("3000000"),
                ReceivableDebtMovementType.PAYMENT, "PAYMENT", "PAY-100", adminUserId, "Cash payment");

        // return = -2m
        debtService.decreaseDebt(customerId, branchId, new BigDecimal("2000000"),
                ReceivableDebtMovementType.RETURN, "RETURN", "RET-100", adminUserId, "Goods returned");

        // Current Debt from Service
        BigDecimal currentDebt = debtService.getCurrentDebt(customerId, branchId);
        assertEquals(0, new BigDecimal("10000000").compareTo(currentDebt), "Total debt should be exactly 10,000,000");

        // DB Invariant Verification (Directly querying PostgreSQL instead of relying on JPA cache)
        // Ensure total amount matches receivable_debt table
        BigDecimal totalMovementAmount = jdbcTemplate.queryForObject(
                "SELECT SUM(amount) FROM receivable_debt_movement WHERE customer_id = ? AND branch_id = ?",
                BigDecimal.class,
                customerId, branchId
        );
        
        BigDecimal actualTotalDebt = jdbcTemplate.queryForObject(
                "SELECT total_debt FROM receivable_debt WHERE customer_id = ? AND branch_id = ?",
                BigDecimal.class,
                customerId, branchId
        );

        assertTrue(totalMovementAmount != null);
        assertTrue(actualTotalDebt != null);
        assertEquals(0, new BigDecimal("10000000").compareTo(totalMovementAmount));
        assertEquals(0, totalMovementAmount.compareTo(actualTotalDebt), "Invariant failed: SUM(movement.amount) != total_debt");
    }
}
