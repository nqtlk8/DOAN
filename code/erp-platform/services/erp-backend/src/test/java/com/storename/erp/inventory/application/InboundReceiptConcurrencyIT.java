package com.storename.erp.inventory.application;

import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.domain.InboundReceipt;
import com.storename.erp.inventory.infrastructure.InboundReceiptRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(properties = { "instance.role=ALL" })
@ActiveProfiles("test")
public class InboundReceiptConcurrencyIT {

    @Autowired
    private InboundReceiptService inboundReceiptService;

    @Autowired
    private InboundReceiptRepository inboundReceiptRepository;

    @Autowired
    private StockOnHandRepository stockOnHandRepository;

    @Test
    public void testConcurrentConfirmReceipt() throws InterruptedException {
        // 1. Create a DRAFT receipt
        InboundReceiptCreateDto dto = new InboundReceiptCreateDto();
        dto.setReceiptCode("NK-" + UUID.randomUUID().toString().substring(0, 5));
        dto.setSupplierId(UUID.randomUUID());
        
        com.storename.erp.inventory.domain.StockOnHand soh = new com.storename.erp.inventory.domain.StockOnHand(2L, 1L);
        stockOnHandRepository.save(soh);
        
        InboundReceiptCreateDto.LineDto line = new InboundReceiptCreateDto.LineDto();
        line.setProductId(2L);
        line.setQuantity(new BigDecimal("10.0"));
        line.setUnitCost(new BigDecimal("100.0"));
        line.setUnitOfMeasure("Box");
        dto.setLines(List.of(line));
        
        UUID receiptId = inboundReceiptService.createDraft(1L, dto);
        
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
                    // Attempt to confirm the receipt
                    inboundReceiptService.confirmReceipt(receiptId, 1L, UUID.randomUUID());
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    System.out.println("Thread got IllegalStateException: " + e.getMessage());
                    if (e.getMessage().contains("Receipt is already CONFIRMED")) {
                        exceptionCount.incrementAndGet();
                    } else {
                        e.printStackTrace();
                    }
                } catch (org.springframework.dao.OptimisticLockingFailureException e) {
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
        Assertions.assertEquals(1, successCount.get(), "Only one thread should successfully confirm the receipt");
        Assertions.assertEquals(4, exceptionCount.get(), "The other threads should throw IllegalStateException after retry");
        
        InboundReceipt updatedReceipt = inboundReceiptRepository.findById(receiptId).orElseThrow();
        Assertions.assertEquals(1, updatedReceipt.getVersion(), "Version should be 1 after one successful confirmation");
    }
}
