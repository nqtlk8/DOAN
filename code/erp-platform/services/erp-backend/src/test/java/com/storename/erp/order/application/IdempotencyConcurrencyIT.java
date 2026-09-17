package com.storename.erp.order.application;

import com.storename.erp.crm.application.CustomerWriteService;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.inventory.infrastructure.StockMovementRepository;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(properties = { "instance.role=ALL" })
@ActiveProfiles("test")
public class IdempotencyConcurrencyIT {

    @Autowired
    private SalesInvoiceService salesInvoiceService;

    @Autowired
    private CustomerWriteService customerWriteService;

    @Autowired
    private SalesInvoiceRepository invoiceRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private com.storename.erp.inventory.infrastructure.StockOnHandRepository stockOnHandRepository;
    
    // Test that concurrent confirm requests on the same DRAFT invoice
    // only result in ONE success, and the others fail gracefully with IllegalStateException
    @Test
    public void testConcurrentConfirmInvoice() throws InterruptedException {
        Customer customer = new Customer();
        customer.setCustomerCode("CUST-" + UUID.randomUUID().toString().substring(0, 5));
        customer.setName("Test Customer");
        customer.setPhone("0909123456");
        customer.setBranchId(1L);
        com.storename.erp.crm.application.dto.CustomerCreateDto dto = new com.storename.erp.crm.application.dto.CustomerCreateDto();
        dto.setCustomerCode("CUST-" + UUID.randomUUID().toString().substring(0, 5));
        dto.setName("Test Customer");
        dto.setPhone("0909123456");
        dto.setEmail("test@test.com");
        dto.setAddress("Test Address");
        dto.setTaxCode("123");
        dto.setBranchId(1L);
        com.storename.erp.crm.application.dto.CustomerResponseDto customerResp = customerWriteService.createCustomer(dto);
        final UUID customerId = customerResp.getId();
        com.storename.erp.inventory.domain.StockOnHand soh = new com.storename.erp.inventory.domain.StockOnHand(1L, 1L);
        stockOnHandRepository.save(soh);
        
        SalesInvoice invoice = new SalesInvoice();
        invoice.setBranchId(1L);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceCode("INV-" + UUID.randomUUID().toString().substring(0, 5));
        
        SalesInvoiceLine line = new SalesInvoiceLine();
        line.setProductId(1L);
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(BigDecimal.TEN);
        line.setLineTotal(BigDecimal.TEN);
        line.setUnitOfMeasure("Cái");
        invoice.addLine(line);
        invoice.calculateTotal();
        
        invoice = invoiceRepository.save(invoice);
        final UUID invoiceId = invoice.getId();
        
        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    // Attempt to confirm the invoice
                    salesInvoiceService.confirmInvoice(invoiceId, UUID.randomUUID(), 1L);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    // Expected for all but one thread because of Retry and check status
                    if ("Only DRAFT can be confirmed".equals(e.getMessage()) || e.getMessage().contains("Invoice is not DRAFT")) {
                        exceptionCount.incrementAndGet();
                    } else {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        latch.countDown();
        doneLatch.await();
        
        // Assertions
        Assertions.assertEquals(1, successCount.get(), "Only one thread should successfully confirm the invoice");
        Assertions.assertEquals(4, exceptionCount.get(), "The other threads should throw IllegalStateException");
        
        SalesInvoice updatedInvoice = invoiceRepository.findById(invoiceId).orElseThrow();
        Assertions.assertEquals(1, updatedInvoice.getVersion(), "Version should be 1 after one successful confirmation");
        
        // Ensure only one stock movement was created for this invoice line
        long movementCount = stockMovementRepository.count();
        // Since the DB is shared among tests, we should check by referenceId
        long specificMovementCount = stockMovementRepository.findAll().stream()
            .filter(m -> m.getRefId().equals(invoiceId.toString()))
            .count();
        Assertions.assertEquals(1, specificMovementCount, "Only one stock movement should be created");
    }
}
