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

/*
 * DB H2 dùng chung giữa các lớp test có cùng cấu hình (Spring cache ApplicationContext), và lớp này
 * ghi dữ liệu không rollback (luồng đồng thời cần commit thật). @DirtiesContext bỏ context sau lớp
 * để lớp sau có DB H2 mới (application-test.yml: testdb_${random.uuid}) — tránh trùng
 * uk_stock_product_branch / mã chứng từ giữa các lớp test.
 */
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
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
                
        // Verify balance_before and balance_after integrity (Append-Only continuous link check).
        // Nối chuỗi theo số dư (balance_before của dòng sau = balance_after của dòng trước), KHÔNG sắp theo
        // created_at: trên Windows đồng hồ JVM chỉ chính xác ~ms nên nhiều dòng ghi liên tiếp trùng thời điểm
        // và thứ tự sắp theo created_at không xác định (test từng lỗi "Chain broken" ngẫu nhiên).
        BigDecimal runningBalance = BigDecimal.ZERO;
        List<ReceivableDebtMovement> remaining = new java.util.ArrayList<>(movements);
        while (!remaining.isEmpty()) {
            final BigDecimal expectedBefore = runningBalance;
            ReceivableDebtMovement movement = remaining.stream()
                    .filter(m -> m.getBalanceBefore().compareTo(expectedBefore) == 0)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Chain broken: no movement starts from running balance " + expectedBefore));
            remaining.remove(movement);

            runningBalance = runningBalance.add(movement.getAmount());

            Assertions.assertEquals(0, runningBalance.compareTo(movement.getBalanceAfter()), 
                "Math error: balanceBefore + amount != balanceAfter");
        }
        
        Assertions.assertEquals(0, runningBalance.compareTo(debt.getTotalDebt()), 
            "Final running balance MUST match the aggregate totalDebt");
    }
}
