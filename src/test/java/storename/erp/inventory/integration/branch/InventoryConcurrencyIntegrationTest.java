package com.storename.erp.inventory.integration.branch;

import com.storename.erp.inventory.application.OutboundReceiptService;
import com.storename.erp.inventory.application.dto.OutboundReceiptCreateDto;
import com.storename.erp.inventory.domain.ReceiptStatus;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.OutboundReceiptRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("branch") // Context branch
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_inventory_test_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "erp.inventory.costing-strategy=weighted-average"
})
public class InventoryConcurrencyIntegrationTest {

    @Autowired
    private OutboundReceiptService outboundService;

    @Autowired
    private StockOnHandRepository stockRepo;

    @Autowired
    private OutboundReceiptRepository outboundRepo;

    private UUID branchId;
    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        stockRepo.deleteAll();
        outboundRepo.deleteAll();

        branchId = UUID.randomUUID();
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();

        // Setup tồn kho ban đầu = 10
        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("10"), "Init");
        stockRepo.save(stock);
    }

    @Test
    void testRaceConditionOnOutboundReceipt() {
        int numberOfThreads = 20;

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            // Chuẩn bị DTO cho mỗi thread
            OutboundReceiptCreateDto dto = new OutboundReceiptCreateDto();
            dto.setReceiptCode("OUT-" + i);
            dto.setReason("Test Race Condition");
            
            OutboundReceiptCreateDto.LineDto line = new OutboundReceiptCreateDto.LineDto();
            line.setProductId(productId);
            line.setQuantity(new BigDecimal("1"));
            line.setUnitOfMeasure("PCS");
            dto.setLines(Collections.singletonList(line));

            // Tạo draft tuần tự để tránh race condition trên việc gen receipt code (ngoài scope test)
            UUID receiptId = outboundService.createDraft(branchId, dto);

            // Xác nhận xuất kho đồng thời
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                outboundService.confirmReceipt(receiptId, branchId, userId);
            }).thenAccept(v -> successCount.incrementAndGet())
              .exceptionally(ex -> {
                  failureCount.incrementAndGet();
                  return null;
              });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 1. Dù bao nhiêu luồng thành công hay thất bại, Invariant phải luôn đúng:
        // finalStock + successCount = 10 (vì mỗi thành công trừ 1 kho)
        StockOnHand finalStock = stockRepo.findByProductIdAndBranchId(productId, branchId).orElseThrow();
        int remainingStock = finalStock.getQuantity().intValue();
        
        assertEquals(10, remainingStock + successCount.get(), "Data Invariant bị vi phạm: Tổng số tồn kho còn lại và số phiếu thành công không khớp!");

        // 2. Không được phép âm
        assertTrue(remainingStock >= 0, "Tồn kho không được phép âm");

        // 3. Số phiếu được lưu là CONFIRMED phải khớp đúng successCount
        long confirmedCount = outboundRepo.findAll().stream()
                .filter(r -> r.getStatus() == ReceiptStatus.CONFIRMED)
                .count();
        assertEquals(successCount.get(), confirmedCount);
    }
}
