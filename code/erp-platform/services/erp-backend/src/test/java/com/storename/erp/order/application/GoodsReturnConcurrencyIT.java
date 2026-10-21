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

/*
 * DB H2 dùng chung giữa các lớp test có cùng cấu hình (Spring cache ApplicationContext), và lớp này
 * ghi dữ liệu không rollback (luồng đồng thời cần commit thật). @DirtiesContext bỏ context sau lớp
 * để lớp sau có DB H2 mới (application-test.yml: testdb_${random.uuid}) — tránh trùng
 * uk_stock_product_branch / mã chứng từ giữa các lớp test.
 */
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
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

    @Autowired
    private com.storename.erp.inventory.infrastructure.CostLayerRepository costLayerRepository;

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
        // Khách dùng chung (branch_id NULL): CustomerWriteService yêu cầu chi nhánh tồn tại, mà test không tạo dòng branch nào.
        dto.setBranchId(null);
        com.storename.erp.crm.application.dto.CustomerResponseDto customerResp = customerWriteService.createCustomer(dto);
        final UUID customerId = customerResp.getId();
        
        com.storename.erp.inventory.domain.StockOnHand soh = new com.storename.erp.inventory.domain.StockOnHand(1L, 1L);
        soh.increase(new BigDecimal("100"), "INIT");
        stockOnHandRepository.save(soh);
        costLayerRepository.save(com.storename.erp.inventory.domain.CostLayer.fromInbound(1L, 1L, new BigDecimal("100"), new BigDecimal("500"), null));
        
        // 2. Tạo và xác nhận một đơn bán 5 sản phẩm (một bước, như luồng bán hàng thật)
        com.storename.erp.order.application.dto.SalesInvoiceLineDto invoiceLine = new com.storename.erp.order.application.dto.SalesInvoiceLineDto();
        invoiceLine.setProductId(1L);
        invoiceLine.setProductName("SP trả hàng");
        invoiceLine.setQuantity(new BigDecimal("5.0"));
        invoiceLine.setUnitPrice(BigDecimal.TEN);
        invoiceLine.setUnitOfMeasure("Cái");
        com.storename.erp.order.application.dto.SalesInvoiceCreateDto invoiceDto = new com.storename.erp.order.application.dto.SalesInvoiceCreateDto();
        invoiceDto.setCustomerId(customerId);
        invoiceDto.setInvoiceCode("INV-" + UUID.randomUUID().toString().substring(0, 5));
        invoiceDto.setLines(List.of(invoiceLine));
        final UUID invoiceId = com.storename.erp.support.SalesInvoiceFlows.createAndConfirm(salesInvoiceService, invoiceDto, 1L, UUID.randomUUID()).getId();
        
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
                    GoodsReturnCreateDto.GoodsReturnLineDto rLine = new GoodsReturnCreateDto.GoodsReturnLineDto();
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
