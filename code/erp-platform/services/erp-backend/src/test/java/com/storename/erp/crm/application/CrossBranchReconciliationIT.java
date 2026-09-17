package com.storename.erp.crm.application;

import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.domain.ReceivableDebt;
import com.storename.erp.crm.domain.ReceivableDebtMovement;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.crm.infrastructure.ReceivableDebtMovementRepository;
import com.storename.erp.crm.infrastructure.ReceivableDebtRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@SpringBootTest(properties = { "instance.role=ALL" })
@ActiveProfiles("test")
public class CrossBranchReconciliationIT {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ReceivableDebtRepository debtRepository;

    @Autowired
    private ReceivableDebtMovementRepository movementRepository;

    @Autowired
    private ReceivableDebtService debtService;

    @Test
    public void testCrossBranchDebtIsolation() {
        // Create Customer - customer belongs to HQ or a specific branch but can interact with multiple branches
        Customer customer = new Customer();
        customer.setName("Cross Branch Customer");
        customer.setCustomerCode("CUST-CROSS-01");
        customer.setPhone("0888888888");
        customer.setBranchId(1L);
        customer = customerRepository.save(customer);

        UUID customerId = customer.getId();
        UUID userId = UUID.randomUUID();

        Long branchA = 1L;
        Long branchB = 2L;

        // 1. Transaction in Branch A
        debtService.increaseDebt(customerId, branchA, BigDecimal.valueOf(10000), 
                com.storename.erp.crm.domain.ReceivableDebtMovementType.INVOICE, "ORDER", UUID.randomUUID().toString(), userId, "Sale at Branch A");

        // 2. Transaction in Branch B
        debtService.increaseDebt(customerId, branchB, BigDecimal.valueOf(5000), 
                com.storename.erp.crm.domain.ReceivableDebtMovementType.INVOICE, "ORDER", UUID.randomUUID().toString(), userId, "Sale at Branch B");

        // 3. Payment in Branch A
        debtService.decreaseDebt(customerId, branchA, BigDecimal.valueOf(3000), 
                com.storename.erp.crm.domain.ReceivableDebtMovementType.PAYMENT, "PAYMENT", UUID.randomUUID().toString(), userId, "Payment at Branch A");

        // Verify Branch A State
        ReceivableDebt debtA = debtRepository.findByCustomerIdAndBranchId(customerId, branchA).orElseThrow();
        Assertions.assertEquals(0, BigDecimal.valueOf(7000).compareTo(debtA.getTotalDebt()), 
                "Branch A debt should be 7000");

        List<ReceivableDebtMovement> movementsA = movementRepository.findByCustomerIdAndBranchIdOrderByCreatedAtDesc(customerId, branchA);
        Assertions.assertEquals(2, movementsA.size(), "Branch A should have exactly 2 movements");

        // Verify Branch B State
        ReceivableDebt debtB = debtRepository.findByCustomerIdAndBranchId(customerId, branchB).orElseThrow();
        Assertions.assertEquals(0, BigDecimal.valueOf(5000).compareTo(debtB.getTotalDebt()), 
                "Branch B debt should be 5000");

        List<ReceivableDebtMovement> movementsB = movementRepository.findByCustomerIdAndBranchIdOrderByCreatedAtDesc(customerId, branchB);
        Assertions.assertEquals(1, movementsB.size(), "Branch B should have exactly 1 movement");

        // Assure total aggregate movements in DB for this customer is 3 across all branches
        List<ReceivableDebtMovement> allMovements = movementRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        Assertions.assertEquals(3, allMovements.size(), "Customer should have exactly 3 movements across all branches");
    }
}
