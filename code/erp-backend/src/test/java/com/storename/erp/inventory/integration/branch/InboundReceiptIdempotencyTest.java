package com.storename.erp.inventory.integration.branch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.domain.IdempotencyRecord;
import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.inventory.api.InboundReceiptController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"branch", "test"})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_inv_test_db4;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.security.user.name=user",
    "spring.security.user.password=password"
})
public class InboundReceiptIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IdempotencyRecordRepository idempotencyRepo;

    @Autowired
    private StockOnHandRepository stockRepo;
    
    @Autowired
    private com.storename.erp.inventory.application.InboundReceiptService inboundService;

    @Autowired
    private com.storename.erp.common.security.JwtTokenProvider jwtTokenProvider;

    private UUID branchId;
    private UUID productId;
    private UUID receiptId;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        idempotencyRepo.deleteAll();
        stockRepo.deleteAll();
        
        branchId = UUID.randomUUID();
        productId = UUID.randomUUID();
        authToken = "Bearer " + jwtTokenProvider.generateToken("testuser", "ADMIN", branchId.toString(), UUID.randomUUID().toString());

        
        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("100"), "Init");
        stockRepo.save(stock);
        
        InboundReceiptCreateDto inDto = new InboundReceiptCreateDto();
        inDto.setReceiptCode("IN-IDEM-001");
        
        InboundReceiptCreateDto.LineDto inLine = new InboundReceiptCreateDto.LineDto();
        inLine.setProductId(productId);
        inLine.setQuantity(new BigDecimal("50"));
        inLine.setUnitCost(new BigDecimal("2000"));
        inLine.setUnitOfMeasure("PCS");
        inDto.setLines(Collections.singletonList(inLine));
        
        receiptId = inboundService.createDraft(branchId, inDto);
    }

    @Test
    void testIdempotency_SameKey_DoesNotDuplicateStock() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        
        CompletableFuture<Void> req1 = CompletableFuture.runAsync(() -> {
            try {
                mockMvc.perform(post("/api/v1/inventory/inbound/" + receiptId + "/confirm")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        CompletableFuture<Void> req2 = CompletableFuture.runAsync(() -> {
            try {
                mockMvc.perform(post("/api/v1/inventory/inbound/" + receiptId + "/confirm")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                // In concurrent requests, one might fail with ConstraintViolation (due to idempotencyKey unique index)
                // or return 200 with cached response. The framework/AOP handles it.
            }
        });
        
        try {
            CompletableFuture.allOf(req1, req2).join();
        } catch (Exception ignored) {}
        
        // Wait briefly for DB to settle if needed, though join() should wait
        Thread.sleep(100);
        
        // Stock should be exactly 100 + 50 = 150 (not 200)
        BigDecimal stockAfter = stockRepo.findByProductIdAndBranchId(productId, branchId).get().getQuantity();
        assertEquals(0, stockAfter.compareTo(new BigDecimal("150")));
        
        // Idempotency record should exist once
        long count = idempotencyRepo.count();
        assertEquals(1, count);
    }
}
