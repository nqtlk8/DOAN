package com.storename.erp.procurement.integration.branch;

import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.procurement.application.StockTransferService;
import com.storename.erp.procurement.application.dto.StockTransferCreateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("branch")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_proc_test_db1;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
public class StockTransferReconciliationTest {

    @Autowired
    private StockTransferService transferService;

    @Autowired
    private StockOnHandRepository stockRepo;

    private UUID fromBranchId;
    private UUID toBranchId;
    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        stockRepo.deleteAll();
        
        fromBranchId = UUID.randomUUID();
        toBranchId = UUID.randomUUID();
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();

        StockOnHand fromStock = new StockOnHand(productId, fromBranchId);
        fromStock.increase(new BigDecimal("100"), "Init fromBranch");
        stockRepo.save(fromStock);
        
        StockOnHand toStock = new StockOnHand(productId, toBranchId);
        toStock.increase(new BigDecimal("50"), "Init toBranch");
        stockRepo.save(toStock);
    }

    @Test
    void testShipAndReceiveTransfer_MaintainsTotalStock() {
        // Tổng tồn kho ban đầu: 100 + 50 = 150
        BigDecimal initialTotal = getStock(fromBranchId).add(getStock(toBranchId));
        assertEquals(0, initialTotal.compareTo(new BigDecimal("150")));

        StockTransferCreateDto dto = new StockTransferCreateDto();
        dto.setToBranchId(toBranchId);
        dto.setTransferCode("TRF-001");
        
        StockTransferCreateDto.LineDto line = new StockTransferCreateDto.LineDto();
        line.setProductId(productId);
        line.setQuantity(new BigDecimal("20"));
        line.setUnitOfMeasure("PCS");
        dto.setLines(Collections.singletonList(line));

        UUID transferId = transferService.requestTransfer(fromBranchId, dto);

        // Ship
        transferService.shipTransfer(transferId, fromBranchId, userId);
        
        // Sau khi ship, kho xuất giảm 20, kho nhập chưa nhận (vẫn là hàng đi đường)
        assertEquals(0, getStock(fromBranchId).compareTo(new BigDecimal("80")));
        assertEquals(0, getStock(toBranchId).compareTo(new BigDecimal("50")));
        
        // Receive
        transferService.receiveTransfer(transferId, toBranchId, userId);
        
        // Sau khi receive, kho nhập tăng 20
        assertEquals(0, getStock(fromBranchId).compareTo(new BigDecimal("80")));
        assertEquals(0, getStock(toBranchId).compareTo(new BigDecimal("70")));
        
        // Tổng tồn kho sau cùng: 80 + 70 = 150
        BigDecimal finalTotal = getStock(fromBranchId).add(getStock(toBranchId));
        assertEquals(0, finalTotal.compareTo(new BigDecimal("150")));
        assertEquals(0, initialTotal.compareTo(finalTotal));
    }
    
    private BigDecimal getStock(UUID branchId) {
        return stockRepo.findByProductIdAndBranchId(productId, branchId)
                .map(StockOnHand::getQuantity)
                .orElse(BigDecimal.ZERO);
    }
}
