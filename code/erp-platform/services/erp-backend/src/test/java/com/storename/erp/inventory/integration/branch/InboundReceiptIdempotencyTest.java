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
    "spring.security.user.password=password",
    "instance.role=BRANCH",
    "branch-id=1001"
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

    private Long branchId;
    private Long productId;
    private UUID receiptId;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        idempotencyRepo.deleteAll();
        stockRepo.deleteAll();
        
        branchId = 1001L;
        productId = (long)(Math.random() * 100000L);
        authToken = "Bearer " + jwtTokenProvider.generateToken(UUID.randomUUID().toString(), "STAFF", branchId.toString(), UUID.randomUUID().toString());

        
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

        // --- Lần 1: Gọi confirm lần đầu -> phải thành công, tồn kho tăng ---
        mockMvc.perform(post("/api/v1/inventory/inbound/" + receiptId + "/confirm")
                .header("Idempotency-Key", idempotencyKey)
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify: tồn kho phải là 100 + 50 = 150
        BigDecimal stockAfterFirst = stockRepo.findByProductIdAndBranchId(productId, branchId)
                .get().getQuantity();
        assertEquals(0, stockAfterFirst.compareTo(new BigDecimal("150")),
                "Stock should be 150 after first confirm");

        // --- Lần 2: Gọi confirm lần 2 cùng key -> cached response, KHÔNG tăng tồn kho ---
        mockMvc.perform(post("/api/v1/inventory/inbound/" + receiptId + "/confirm")
                .header("Idempotency-Key", idempotencyKey)
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify: tồn kho vẫn phải là 150 (KHÔNG tăng thêm)
        BigDecimal stockAfterSecond = stockRepo.findByProductIdAndBranchId(productId, branchId)
                .get().getQuantity();
        assertEquals(0, stockAfterSecond.compareTo(new BigDecimal("150")),
                "Stock should STILL be 150 after idempotent retry");

        // Verify: chỉ có đúng 1 bản ghi idempotency
        long count = idempotencyRepo.count();
        assertEquals(1, count, "Should have exactly 1 idempotency record");
    }
}
