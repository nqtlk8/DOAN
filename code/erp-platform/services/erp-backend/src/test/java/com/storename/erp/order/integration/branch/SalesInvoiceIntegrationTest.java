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

    private Long branchId;
    private Customer customer;
    private Long productId;

    @Autowired
    private com.storename.erp.crm.infrastructure.ReceivableDebtRepository debtRepo;

    @Autowired
    private com.storename.erp.crm.infrastructure.ReceivableDebtMovementRepository movementRepo;

    @BeforeEach
    void setUp() {
        movementRepo.deleteAll();
        debtRepo.deleteAll();
        customerRepo.deleteAll();
        stockRepo.deleteAll();
        invoiceRepo.deleteAll();
        
        branchId = (long)(Math.random() * 100000L);
        productId = (long)(Math.random() * 100000L);

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
        debtService.increaseDebt(customer.getId(), branchId, new BigDecimal("5000"),
                com.storename.erp.crm.domain.ReceivableDebtMovementType.ADJUSTMENT, "TEST", "TEST", UUID.randomUUID(), "test");

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
            invoiceService.confirmInvoice(draft.getId(), UUID.randomUUID(), 1L);
        });

        // Debt should not increase
        BigDecimal debtAfter = debtService.getCurrentDebt(customer.getId(), branchId);
        assertEquals(0, debtAfter.compareTo(BigDecimal.ZERO));
    }

    @Test
    void confirmInvoice_exceedsStock() {
        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setInvoiceCode("INV-TEST-EXCEED");
        dto.setCustomerId(customer.getId());
        dto.setPaymentMethod(PaymentMethod.CASH);

        SalesInvoiceLineDto line = new SalesInvoiceLineDto();
        line.setProductId(productId);
        line.setProductName("Test Product");
        line.setQuantity(new BigDecimal("150")); // Exceeds 100 stock
        line.setUnitPrice(new BigDecimal("1000"));
        line.setUnitOfMeasure("PCS");
        dto.setLines(Collections.singletonList(line));

        SalesInvoice draft = invoiceService.createDraft(dto, branchId);
        invoiceService.confirmInvoice(draft.getId(), UUID.randomUUID(), branchId);

        StockOnHand stock = stockRepo.findByBranchId(branchId).get(0);
        assertEquals(-50, stock.getQuantity().intValue());
    }

    @Test
    void confirmInvoice_multipleProducts() {
        Long productId2 = (long)(Math.random() * 100000L);
        StockOnHand stock2 = new StockOnHand(productId2, branchId);
        stock2.increase(new BigDecimal("50"), "Init");
        stockRepo.save(stock2);

        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setInvoiceCode("INV-TEST-MULTI");
        dto.setCustomerId(customer.getId());
        dto.setPaymentMethod(PaymentMethod.CASH);

        SalesInvoiceLineDto line1 = new SalesInvoiceLineDto();
        line1.setProductId(productId);
        line1.setProductName("Test Product 1");
        line1.setQuantity(new BigDecimal("30"));
        line1.setUnitPrice(new BigDecimal("1000"));
        line1.setUnitOfMeasure("PCS");

        SalesInvoiceLineDto line2 = new SalesInvoiceLineDto();
        line2.setProductId(productId2);
        line2.setProductName("Test Product 2");
        line2.setQuantity(new BigDecimal("20"));
        line2.setUnitPrice(new BigDecimal("2000"));
        line2.setUnitOfMeasure("PCS");

        dto.setLines(java.util.Arrays.asList(line1, line2));

        SalesInvoice draft = invoiceService.createDraft(dto, branchId);
        invoiceService.confirmInvoice(draft.getId(), UUID.randomUUID(), branchId);

        assertEquals(70, stockRepo.findByProductIdAndBranchId(productId, branchId).get().getQuantity().intValue());
        assertEquals(30, stockRepo.findByProductIdAndBranchId(productId2, branchId).get().getQuantity().intValue());
    }
}
