package com.storename.erp.crm.infrastructure;

import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.domain.ReceivableDebtMovement;
import com.storename.erp.crm.domain.ReceivableDebtMovementType;
import com.storename.erp.test.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres-it")
@Transactional // rollback toàn bộ fixture sau mỗi test
public class ReceivableDebtMovementRepositoryIT extends PostgresIntegrationTest {

    @Autowired
    private ReceivableDebtMovementRepository movementRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Repository là append-only (chỉ có save), không có saveAndFlush — dùng EntityManager để flush
     * xuống DB ngay trong test, nhờ đó lỗi ràng buộc lộ ra tại đúng chỗ.
     */
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        // Prepare dummy customer for foreign key requirements
        testCustomer = new Customer();
        testCustomer.setId(UUID.randomUUID());
        testCustomer.setName("IT Customer");
        testCustomer.setPhone("0999999999");
        // Chèn bằng SQL, đủ cột NOT NULL của bảng customer (db/migration/V3__crm.sql).
        jdbcTemplate.update(
                "INSERT INTO customer (id, customer_code, name, phone, branch_id, version, is_deleted) VALUES (?, ?, ?, ?, ?, 0, false)",
                testCustomer.getId(), "IT-MOV-" + testCustomer.getId().toString().substring(0, 8),
                testCustomer.getName(), testCustomer.getPhone(), 1L
        );
    }

    @Test
    void shouldSaveAndGenerateId() {
        ReceivableDebtMovement movement = ReceivableDebtMovement.create(
                1L,
                testCustomer,
                ReceivableDebtMovementType.INVOICE,
                new BigDecimal("5000000.0000"),
                BigDecimal.ZERO,
                new BigDecimal("5000000.0000"),
                "SALE",
                "INV-001",
                null,
                null,
                "IDEMPOTENCY-KEY-1"
        );

        ReceivableDebtMovement saved = movementRepository.save(movement);
        entityManager.flush();

        assertNotNull(saved.getId());
        
        java.util.List<ReceivableDebtMovement> founds = movementRepository.findByCustomerIdAndBranchIdOrderByCreatedAtDesc(testCustomer.getId(), 1L);
        assertFalse(founds.isEmpty());
        assertEquals(0, new BigDecimal("5000000.0000").compareTo(founds.get(0).getAmount()));
    }

    @Test
    void shouldEnforceIdempotencyKeyUniqueConstraint() {
        String key = "SAME-IDEMPOTENCY-KEY";
        ReceivableDebtMovement movement1 = ReceivableDebtMovement.create(
                1L, testCustomer, ReceivableDebtMovementType.PAYMENT, new BigDecimal("100"),
                BigDecimal.ZERO, new BigDecimal("100"), "PAYMENT", "PAY-001", null, null, key
        );
        movementRepository.save(movement1);
        entityManager.flush();

        ReceivableDebtMovement movement2 = ReceivableDebtMovement.create(
                1L, testCustomer, ReceivableDebtMovementType.PAYMENT, new BigDecimal("100"),
                BigDecimal.ZERO, new BigDecimal("100"), "PAYMENT", "PAY-001", null, null, key
        );

        // EntityManager.flush() ném exception của JPA/Hibernate (không qua lớp dịch exception của Spring),
        // nên kiểm tra nguyên nhân gốc: đúng ràng buộc uk_debt_movement_idempotency của DB đã chặn.
        Exception ex = assertThrows(jakarta.persistence.PersistenceException.class, () -> {
            movementRepository.save(movement2);
            entityManager.flush();
        });
        Throwable root = ex;
        while (root.getCause() != null) root = root.getCause();
        org.junit.jupiter.api.Assertions.assertTrue(root.getMessage().contains("uk_debt_movement_idempotency"),
                "Phải bị chặn bởi uk_debt_movement_idempotency, thực tế: " + root.getMessage());
    }
}
