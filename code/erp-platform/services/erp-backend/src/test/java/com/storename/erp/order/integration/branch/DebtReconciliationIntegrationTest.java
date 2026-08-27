package com.storename.erp.order.integration.branch;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.order.application.SalesInvoiceService;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceLineDto;
import com.storename.erp.order.domain.PaymentMethod;
import com.storename.erp.order.domain.SalesInvoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("branch")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_order_test_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
public class DebtReconciliationIntegrationTest {

    @Autowired
    private SalesInvoiceService invoiceService;

    @Autowired
    private ReceivableDebtService debtService;

    @Autowired
    private CustomerRepository customerRepo;
    
    @Autowired
    private StockOnHandRepository stockRepo;

    private Long branchId;
    private Customer customer;
    private Long productId;

    @BeforeEach
    void setUp() {
        customerRepo.deleteAll();
        stockRepo.deleteAll();
        
        branchId = (long)(Math.random() * 100000L);
        productId = (long)(Math.random() * 100000L);

        customer = new Customer();
        customer.setCustomerCode("CUST-DEBT-01");
        customer.setName("Test Customer");
        customer = customerRepo.save(customer);

        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("100"), "Init");
        stockRepo.save(stock);
    }

    @Test
    void testCreateAndConfirmInvoice_UpdatesDebtCorrectly() {
        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setInvoiceCode("INV-DEBT-001");
        dto.setCustomerId(customer.getId());
        dto.setPaymentMethod(PaymentMethod.CREDIT);

        SalesInvoiceLineDto line = new SalesInvoiceLineDto();
        line.setProductId(productId);
        line.setProductName("Test Product");
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("1000"));
        line.setUnitOfMeasure("PCS");
        dto.setLines(Collections.singletonList(line));

        SalesInvoice draft = invoiceService.createDraft(dto, branchId);
        
        BigDecimal debtBefore = debtService.getCurrentDebt(customer.getId(), branchId);
        assertEquals(0, debtBefore.compareTo(BigDecimal.ZERO));

        SalesInvoice confirmed = invoiceService.confirmInvoice(draft.getId(), UUID.randomUUID(), branchId);

        BigDecimal debtAfter = debtService.getCurrentDebt(customer.getId(), branchId);
        assertEquals(0, debtAfter.compareTo(new BigDecimal("2000")));
        assertEquals(0, confirmed.getPreviousDebt().compareTo(BigDecimal.ZERO));
        assertEquals(0, confirmed.getRemainingDebt().compareTo(new BigDecimal("2000")));
    }
}
