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
@Transactional // To rollback changes after test, but wait, DB constraint testing might need manual flush if transaction caches
public class ReceivableDebtMovementRepositoryIT extends PostgresIntegrationTest {

    @Autowired
    private ReceivableDebtMovementRepository movementRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        // Prepare dummy customer for foreign key requirements
        testCustomer = new Customer();
        testCustomer.setId(UUID.randomUUID());
        testCustomer.setName("IT Customer");
        testCustomer.setPhone("0999999999");
        // Use raw SQL to insert to bypass potential other mandatory fields in Customer entity
        jdbcTemplate.update(
                "INSERT INTO customer (id, name, phone, branch_id) VALUES (?, ?, ?, ?)",
                testCustomer.getId(), testCustomer.getName(), testCustomer.getPhone(), 1L
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

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            movementRepository.save(movement2);
            entityManager.flush();
        });
    }
}
