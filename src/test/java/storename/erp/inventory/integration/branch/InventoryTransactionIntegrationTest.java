package com.storename.erp.inventory.integration.branch;

import com.storename.erp.inventory.application.OutboundReceiptService;
import com.storename.erp.inventory.application.dto.OutboundReceiptCreateDto;
import com.storename.erp.inventory.domain.OutboundReceipt;
import com.storename.erp.inventory.domain.ReceiptStatus;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.domain.event.StockDecreasedEvent;
import com.storename.erp.inventory.infrastructure.OutboundReceiptRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("branch")
@RecordApplicationEvents
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_inventory_tx_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
public class InventoryTransactionIntegrationTest {

    @Autowired
    private OutboundReceiptService outboundService;

    @SpyBean
    private StockOnHandRepository stockRepo;

    @Autowired
    private OutboundReceiptRepository outboundRepo;

    @Autowired
    private ApplicationEvents events;

    private UUID branchId;
    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        stockRepo.deleteAll();
        outboundRepo.deleteAll();
        events.clear();

        branchId = UUID.randomUUID();
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();

        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("10"), "Init");
        stockRepo.save(stock); // Lời gọi save này sẽ gọi phương thức thực sự vì nó nằm ở Setup (có thể Mockito reset sau setup nhưng cứ chắc chắn)
    }

    @Test
    void testRollbackAtomicity() {
        // Prepare DRAFT receipt
        OutboundReceiptCreateDto dto = new OutboundReceiptCreateDto();
        dto.setReceiptCode("OUT-FAIL");
        dto.setReason("Test Rollback");
        
        OutboundReceiptCreateDto.LineDto line = new OutboundReceiptCreateDto.LineDto();
        line.setProductId(productId);
        line.setQuantity(new BigDecimal("3"));
        line.setUnitOfMeasure("PCS");
        dto.setLines(Collections.singletonList(line));

        UUID receiptId = outboundService.createDraft(branchId, dto);

        // Force an exception by spying on StockOnHandRepository
        Mockito.doThrow(new RuntimeException("Simulated DB Failure"))
               .when(stockRepo).save(any(StockOnHand.class));

        // Act
        assertThrows(RuntimeException.class, () -> {
            outboundService.confirmReceipt(receiptId, branchId, userId);
        });

        // Assert
        // 1. Stock quantity must remain 10 (rolled back)
        StockOnHand finalStock = stockRepo.findByProductIdAndBranchId(productId, branchId).orElseThrow();
        assertEquals(0, finalStock.getQuantity().compareTo(new BigDecimal("10")), "Tồn kho không được bị trừ");

        // 2. Receipt must still be DRAFT (not CONFIRMED)
        OutboundReceipt receipt = outboundRepo.findById(receiptId).orElseThrow();
        assertEquals(ReceiptStatus.DRAFT, receipt.getStatus());

        // 3. No StockDecreasedEvent published
        long eventCount = events.stream(StockDecreasedEvent.class).count();
        assertEquals(0, eventCount, "Không được có event nào lọt ra ngoài khi transaction rollback");
    }
}
