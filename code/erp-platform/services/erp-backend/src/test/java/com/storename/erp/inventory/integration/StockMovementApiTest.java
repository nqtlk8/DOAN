package com.storename.erp.inventory.integration;

import com.storename.erp.common.security.JwtTokenProvider;
import com.storename.erp.inventory.domain.StockMovement;
import com.storename.erp.inventory.infrastructure.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles({"branch", "test"})
@TestPropertySource(properties = {
    "instance.role=BRANCH",
    "branch-id=1001"
})
public class StockMovementApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockMovementRepository movementRepo;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String authToken;
    private Long productId = 555L;

    @BeforeEach
    void setUp() {
        movementRepo.deleteAll();
        authToken = "Bearer " + jwtTokenProvider.generateToken(UUID.randomUUID().toString(), "STAFF", "1001", UUID.randomUUID().toString());

        UUID userId = UUID.randomUUID();
        // Inbound
        movementRepo.save(StockMovement.inbound(productId, 1001L, new BigDecimal("100"), UUID.randomUUID().toString(), UUID.randomUUID(), userId));
        // Sale
        movementRepo.save(StockMovement.sale(productId, 1001L, new BigDecimal("30"), UUID.randomUUID().toString(), UUID.randomUUID(), userId));
    }

    @Test
    void getMovements_returnsChronologicalHistory() throws Exception {
        mockMvc.perform(get("/api/v1/stock-movements")
                .param("productId", productId.toString())
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].movementType").value("INBOUND"))
                .andExpect(jsonPath("$.data[1].movementType").value("SALE"));
    }

    @Test
    void getMovements_emptyProduct() throws Exception {
        mockMvc.perform(get("/api/v1/stock-movements")
                .param("productId", "999")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}

