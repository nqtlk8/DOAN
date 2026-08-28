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
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("branch")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_order_test_db2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
public class SalesInvoiceIntegrationTest {

    @Autowired
    private SalesInvoiceService invoiceService;

    @Autowired
    private ReceivableDebtService debtService;

    @Autowired
    private CustomerRepository customerRepo;
    
    @Autowired
    private StockOnHandRepository stockRepo;
    
    @Autowired
    private SalesInvoiceRepository invoiceRepo;

    private UUID branchId;
    private Customer customer;
    private UUID productId;

    @BeforeEach
    void setUp() {
        customerRepo.deleteAll();
        stockRepo.deleteAll();
        invoiceRepo.deleteAll();
        
        branchId = UUID.randomUUID();
        productId = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerCode("CUST-INV-01");
        customer.setName("Test Customer");
        customer = customerRepo.save(customer);

        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("100"), "Init");
        stockRepo.save(stock);
    }

    @Test
    void testDebtSnapshotImmutability() {
        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setInvoiceCode("INV-TEST-002");
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
        SalesInvoice confirmed = invoiceService.confirmInvoice(draft.getId(), UUID.randomUUID(), branchId);

        assertEquals(0, confirmed.getPreviousDebt().compareTo(BigDecimal.ZERO));
        assertEquals(0, confirmed.getRemainingDebt().compareTo(new BigDecimal("2000")));

        // Modify debt outside of invoice
        debtService.increaseDebt(customer.getId(), branchId, new BigDecimal("5000"));

        // Refetch invoice
        SalesInvoice refetched = invoiceRepo.findById(confirmed.getId()).orElseThrow();
        
        // Snapshot should remain unchanged
        assertEquals(0, refetched.getPreviousDebt().compareTo(BigDecimal.ZERO));
        assertEquals(0, refetched.getRemainingDebt().compareTo(new BigDecimal("2000")));
    }

    @Test
    void testTransactionRollbackOnError() {
        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setInvoiceCode("INV-TEST-003");
        dto.setCustomerId(customer.getId());
        dto.setPaymentMethod(PaymentMethod.CASH);

        SalesInvoiceLineDto line = new SalesInvoiceLineDto();
        line.setProductId(productId);
        line.setProductName("Test Product");
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("1000"));
        line.setUnitOfMeasure("PCS");
        dto.setLines(Collections.singletonList(line));

        SalesInvoice draft = invoiceService.createDraft(dto, branchId);
        
        // Simulating error by passing a wrong branchId which will throw SecurityException
        assertThrows(SecurityException.class, () -> {
            invoiceService.confirmInvoice(draft.getId(), UUID.randomUUID(), UUID.randomUUID());
        });

        // Debt should not increase
        BigDecimal debtAfter = debtService.getCurrentDebt(customer.getId(), branchId);
        assertEquals(0, debtAfter.compareTo(BigDecimal.ZERO));
    }
}
