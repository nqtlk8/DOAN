package com.storename.erp.order.application;

import com.storename.erp.crm.application.CustomerWriteService;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.inventory.infrastructure.StockMovementRepository;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import com.storename.erp.order.application.dto.GoodsReturnCreateDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(properties = { "instance.role=ALL" })
@ActiveProfiles("test")
public class GoodsReturnConcurrencyIT {

    @Autowired
    private SalesInvoiceService salesInvoiceService;
    
    @Autowired
    private GoodsReturnService goodsReturnService;

    @Autowired
    private CustomerWriteService customerWriteService;

    @Autowired
    private SalesInvoiceRepository invoiceRepository;

    @Autowired
    private com.storename.erp.inventory.infrastructure.StockOnHandRepository stockOnHandRepository;

    // Test that concurrent confirm return requests on the same invoice
    // for the maximum amount only succeed once due to pessimistic lock.
    @Test
    public void testConcurrentConfirmReturn_TOCTOU() throws InterruptedException {
        // 1. Create a customer
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
        soh.increase(new BigDecimal("100"), "INIT");
        stockOnHandRepository.save(soh);
        
        // 2. Create and confirm an invoice of 5 items
        SalesInvoice invoice = new SalesInvoice();
        invoice.setBranchId(1L);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceCode("INV-" + UUID.randomUUID().toString().substring(0, 5));
        
        SalesInvoiceLine line = new SalesInvoiceLine();
        line.setProductId(1L);
        line.setQuantity(new BigDecimal("5.0"));
        line.setUnitPrice(BigDecimal.TEN);
        line.setLineTotal(new BigDecimal("50.0"));
        line.setUnitOfMeasure("Cái");
        invoice.addLine(line);
        invoice.calculateTotal();
        
        invoice = invoiceRepository.save(invoice);
        final UUID invoiceId = invoice.getId();
        
        salesInvoiceService.confirmInvoice(invoiceId, UUID.randomUUID(), 1L);
        
        int numThreads = 3; // 3 threads trying to return 5 items each (total 15, max 5)
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    // Create DRAFT returns BEFORE the race starts to test confirm TOCTOU
                    GoodsReturnCreateDto returnDto = new GoodsReturnCreateDto();
                    returnDto.setInvoiceId(invoiceId);
                    returnDto.setCustomerId(customerId);
                    GoodsReturnCreateDto.LineDto rLine = new GoodsReturnCreateDto.LineDto();
                    rLine.setProductId(1L);
                    rLine.setQuantity(new BigDecimal("5.0"));
                    rLine.setUnitPrice(BigDecimal.TEN);
                    rLine.setUnitOfMeasure("Cái");
                    returnDto.setLines(List.of(rLine));
                    
                    UUID returnId = goodsReturnService.createDraft(1L, returnDto);
                    
                    latch.await();
                    // Attempt to confirm the return (triggers pessimistic lock + limit check)
                    goodsReturnService.confirmReturn(returnId, 1L, UUID.randomUUID());
                    successCount.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    System.out.println("Thread got IllegalArgumentException: " + e.getMessage());
                    if (e.getMessage().contains("Cannot return more than invoiced") || e.getMessage().contains("Total returned quantity exceeds invoice sold quantity")) {
                        exceptionCount.incrementAndGet();
                    } else {
                        e.printStackTrace();
                    }
                } catch (OptimisticLockingFailureException e) {
                    System.out.println("Thread got OptimisticLockingFailureException: " + e.getMessage());
                    exceptionCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("Thread got general Exception: " + e.getMessage());
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
        Assertions.assertEquals(1, successCount.get(), "Only one thread should successfully confirm the return for the max amount");
        Assertions.assertEquals(2, exceptionCount.get(), "The other threads should throw limit violation due to pessimistic lock");
    }
}
