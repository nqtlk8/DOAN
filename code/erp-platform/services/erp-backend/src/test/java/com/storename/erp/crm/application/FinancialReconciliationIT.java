package com.storename.erp.crm.application;

import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.domain.ReceivableDebt;
import com.storename.erp.crm.domain.ReceivableDebtMovement;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.crm.infrastructure.ReceivableDebtMovementRepository;
import com.storename.erp.crm.infrastructure.ReceivableDebtRepository;
import com.storename.erp.order.application.SalesInvoiceService;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.inventory.domain.StockOnHand;
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
public class FinancialReconciliationIT {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ReceivableDebtRepository debtRepository;

    @Autowired
    private ReceivableDebtMovementRepository movementRepository;

    @Autowired
    private ReceivableDebtService debtService;

    @Test
    public void testDebtReconciliation() {
        // Create Customer
        Customer customer = new Customer();
        customer.setName("Reconciliation Test Customer");
        customer.setCustomerCode("CUST-REC-01");
        customer.setPhone("0999999999");
        customer.setBranchId(1L);
        customer = customerRepository.save(customer);

        UUID customerId = customer.getId();
        Long branchId = 1L;
        UUID userId = UUID.randomUUID();

        // 1. Initial debt
        debtService.increaseDebt(customerId, branchId, BigDecimal.valueOf(15000), 
                com.storename.erp.crm.domain.ReceivableDebtMovementType.INVOICE, "ORDER", UUID.randomUUID().toString(), userId, "Initial sale");

        // 2. Partial payment
        debtService.decreaseDebt(customerId, branchId, BigDecimal.valueOf(5000), 
                com.storename.erp.crm.domain.ReceivableDebtMovementType.PAYMENT, "PAYMENT", UUID.randomUUID().toString(), userId, "First payment");

        // 3. Another sale
        debtService.increaseDebt(customerId, branchId, BigDecimal.valueOf(20000), 
                com.storename.erp.crm.domain.ReceivableDebtMovementType.INVOICE, "ORDER", UUID.randomUUID().toString(), userId, "Second sale");

        // 4. Refund / Return
        debtService.decreaseDebt(customerId, branchId, BigDecimal.valueOf(3000), 
                com.storename.erp.crm.domain.ReceivableDebtMovementType.RETURN, "RETURN", UUID.randomUUID().toString(), userId, "Refund item");

        // 5. Final payment
        debtService.decreaseDebt(customerId, branchId, BigDecimal.valueOf(10000), 
                com.storename.erp.crm.domain.ReceivableDebtMovementType.PAYMENT, "PAYMENT", UUID.randomUUID().toString(), userId, "Second payment");

        // Verify state
        ReceivableDebt debt = debtRepository.findByCustomerIdAndBranchId(customerId, branchId).orElseThrow();
        
        // Expected Balance: 15000 - 5000 + 20000 - 3000 - 10000 = 17000
        BigDecimal expectedBalance = BigDecimal.valueOf(17000);
        Assertions.assertEquals(0, expectedBalance.compareTo(debt.getTotalDebt()), 
                "Total debt should match the sum of arithmetic operations");

        // Fetch all movements
        List<ReceivableDebtMovement> movements = movementRepository.findByCustomerIdAndBranchIdOrderByCreatedAtDesc(customerId, branchId);
        
        // Total movements = 5
        Assertions.assertEquals(5, movements.size(), "Should have exactly 5 movements");

        // Sum movements amount (since they are signed, we just sum them directly)
        BigDecimal sumOfMovements = BigDecimal.ZERO;
        for (ReceivableDebtMovement movement : movements) {
            sumOfMovements = sumOfMovements.add(movement.getAmount());
        }

        Assertions.assertEquals(0, expectedBalance.compareTo(sumOfMovements), 
                "SUM(amount) in ledger MUST strictly equal the totalDebt on the entity");
                
        // Verify balance_before and balance_after integrity (Append-Only continuous link check)
        BigDecimal runningBalance = BigDecimal.ZERO;
        // Since list is descending, we iterate in reverse or just fetch ascending
        movements.sort((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt())); // sort ascending
        
        for (ReceivableDebtMovement movement : movements) {
            Assertions.assertEquals(0, runningBalance.compareTo(movement.getBalanceBefore()), 
                "Chain broken: balanceBefore does not match previous running balance");
            
            runningBalance = runningBalance.add(movement.getAmount());
            
            Assertions.assertEquals(0, runningBalance.compareTo(movement.getBalanceAfter()), 
                "Math error: balanceBefore + amount != balanceAfter");
        }
        
        Assertions.assertEquals(0, runningBalance.compareTo(debt.getTotalDebt()), 
            "Final running balance MUST match the aggregate totalDebt");
    }
}
