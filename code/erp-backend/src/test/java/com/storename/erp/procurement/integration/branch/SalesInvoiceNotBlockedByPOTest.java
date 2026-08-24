package com.storename.erp.procurement.integration.branch;

import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.order.application.SalesInvoiceService;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceLineDto;
import com.storename.erp.order.domain.PaymentMethod;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.domain.StockOnHand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("branch")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_proc_test_db5;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
public class SalesInvoiceNotBlockedByPOTest {

    @Autowired
    private SalesInvoiceService invoiceService;

    @Autowired
    private CustomerRepository customerRepo;
    
    @Autowired
    private StockOnHandRepository stockRepo;

    private UUID branchId;
    private Customer customer;
    private UUID productId;

    @BeforeEach
    void setUp() {
        customerRepo.deleteAll();
        stockRepo.deleteAll();
        
        branchId = UUID.randomUUID();
        productId = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerCode("CUST-ISO-01");
        customer.setName("Isolation Customer");
        customer = customerRepo.save(customer);

        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("100"), "Init");
        stockRepo.save(stock);
    }

    @Test
    void testSalesInvoice_StillWorksIndependently() {
        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setInvoiceCode("INV-ISO-001");
        dto.setCustomerId(customer.getId());
        dto.setPaymentMethod(PaymentMethod.CASH);

        SalesInvoiceLineDto line = new SalesInvoiceLineDto();
        line.setProductId(productId);
        line.setProductName("Isolated Product");
        line.setQuantity(new BigDecimal("20"));
        line.setUnitPrice(new BigDecimal("1000"));
        line.setUnitOfMeasure("PCS");
        dto.setLines(Collections.singletonList(line));

        SalesInvoice draft = invoiceService.createDraft(dto, branchId);
        SalesInvoice confirmed = invoiceService.confirmInvoice(draft.getId(), UUID.randomUUID(), branchId);

        assertNotNull(confirmed.getConfirmedAt());
        
        BigDecimal stockAfter = stockRepo.findByProductIdAndBranchId(productId, branchId).get().getQuantity();
        assertEquals(0, stockAfter.compareTo(new BigDecimal("80")));
    }
}
