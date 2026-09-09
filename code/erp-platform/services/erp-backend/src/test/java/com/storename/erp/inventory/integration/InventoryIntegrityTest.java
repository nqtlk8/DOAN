package com.storename.erp.inventory.integration;

import com.storename.erp.inventory.application.FifoCostService;
import com.storename.erp.inventory.application.InboundReceiptService;
import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.domain.CostLayer;
import com.storename.erp.inventory.domain.StockMovement;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.CostLayerRepository;
import com.storename.erp.inventory.infrastructure.StockMovementRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.inventory.api.InventoryFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles({"branch", "test"})
public class InventoryIntegrityTest {

    @Autowired private InboundReceiptService inboundService;
    @Autowired private InventoryFacade inventoryFacade;
    @Autowired private StockOnHandRepository stockRepo;
    @Autowired private StockMovementRepository movementRepo;
    @Autowired private CostLayerRepository costLayerRepo;
    @Autowired private FifoCostService fifoCostService;

    private Long branchId = 1L;
    private Long productId = 100L;
    private UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        stockRepo.deleteAll();
        movementRepo.deleteAll();
        costLayerRepo.deleteAll();
    }

    @Test
    public void inbound_createsMovementAndCostLayer() {
        InboundReceiptCreateDto dto = new InboundReceiptCreateDto();
        dto.setReceiptCode("NK001");
        InboundReceiptCreateDto.LineDto lineDto = new InboundReceiptCreateDto.LineDto();
        lineDto.setProductId(productId);
        lineDto.setQuantity(new BigDecimal("100"));
        lineDto.setUnitCost(new BigDecimal("10000"));
        lineDto.setUnitOfMeasure("Cai");
        dto.setLines(List.of(lineDto));
        UUID receiptId = inboundService.createDraft(branchId, dto);
        inboundService.confirmReceipt(receiptId, branchId, userId);

        List<StockMovement> movements = movementRepo.findByProductIdAndBranchIdOrderByCreatedAtAsc(productId, branchId);
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getQuantity()).isEqualByComparingTo("100");
        assertThat(movements.get(0).getMovementType().name()).isEqualTo("INBOUND");

        List<CostLayer> layers = costLayerRepo.findAvailableForFifoWithLock(productId, branchId);
        assertThat(layers).hasSize(1);
        assertThat(layers.get(0).getRemainingQty()).isEqualByComparingTo("100");
        assertThat(layers.get(0).getUnitCost()).isEqualByComparingTo("10000");

        List<StockOnHand> stocks = stockRepo.findByBranchId(branchId);
        assertThat(stocks).hasSize(1);
        assertThat(stocks.get(0).getQuantity()).isEqualByComparingTo("100");
    }

    @Test
    public void sumMovement_equalsStockOnHand() {
        // Inbound 100
        InboundReceiptCreateDto dto = new InboundReceiptCreateDto();
        InboundReceiptCreateDto.LineDto lineDto = new InboundReceiptCreateDto.LineDto();
        lineDto.setProductId(productId);
        lineDto.setQuantity(new BigDecimal("100"));
        lineDto.setUnitCost(new BigDecimal("10000"));
        lineDto.setUnitOfMeasure("Cai");
        dto.setLines(List.of(lineDto));
        UUID receiptId = inboundService.createDraft(branchId, dto);
        inboundService.confirmReceipt(receiptId, branchId, userId);

        // Sale 30
        inventoryFacade.recordSaleAndGetCost(productId, branchId, new BigDecimal("30"), UUID.randomUUID().toString(), UUID.randomUUID(), userId);

        BigDecimal sumMovement = movementRepo.findByProductIdAndBranchIdOrderByCreatedAtAsc(productId, branchId).stream()
                .map(StockMovement::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StockOnHand stock = stockRepo.findByBranchId(branchId).get(0);
        
        assertThat(sumMovement).isEqualByComparingTo("70");
        assertThat(stock.getQuantity()).isEqualByComparingTo("70");
    }

    @Test
    public void fifo_consumesOldestFirst() {
        InboundReceiptCreateDto dto1 = new InboundReceiptCreateDto();
        InboundReceiptCreateDto.LineDto lineDto1 = new InboundReceiptCreateDto.LineDto();
        lineDto1.setProductId(productId);
        lineDto1.setQuantity(new BigDecimal("100"));
        lineDto1.setUnitCost(new BigDecimal("10000"));
        lineDto1.setUnitOfMeasure("Cai");
        dto1.setLines(List.of(lineDto1));
        UUID receiptId1 = inboundService.createDraft(branchId, dto1);
        inboundService.confirmReceipt(receiptId1, branchId, userId);

        InboundReceiptCreateDto dto2 = new InboundReceiptCreateDto();
        InboundReceiptCreateDto.LineDto lineDto2 = new InboundReceiptCreateDto.LineDto();
        lineDto2.setProductId(productId);
        lineDto2.setQuantity(new BigDecimal("50"));
        lineDto2.setUnitCost(new BigDecimal("12000"));
        lineDto2.setUnitOfMeasure("Cai");
        dto2.setLines(List.of(lineDto2));
        UUID receiptId2 = inboundService.createDraft(branchId, dto2);
        inboundService.confirmReceipt(receiptId2, branchId, userId);

        InventoryFacade.SaleCostResult costResult = inventoryFacade.recordSaleAndGetCost(productId, branchId, new BigDecimal("80"), UUID.randomUUID().toString(), UUID.randomUUID(), userId);

        assertThat(costResult.unitCostSnapshot()).isEqualByComparingTo("10000");
        assertThat(costResult.costBasis()).isEqualTo("NORMAL");

        List<CostLayer> layers = costLayerRepo.findAvailableForFifoWithLock(productId, branchId);
        // Note: layer1 will have 20 remaining, layer2 will have 50 remaining
        // findAvailableForFifoWithLock might return them in order
        assertThat(layers).hasSize(2);
        assertThat(layers.get(0).getRemainingQty()).isEqualByComparingTo("20");
        assertThat(layers.get(1).getRemainingQty()).isEqualByComparingTo("50");
    }

    @Test
    public void negativeStock_createsMovementWithNoLayer() {
        InventoryFacade.SaleCostResult costResult = inventoryFacade.recordSaleAndGetCost(productId, branchId, new BigDecimal("50"), UUID.randomUUID().toString(), UUID.randomUUID(), userId);

        assertThat(costResult.costBasis()).isEqualTo("NO_LAYER");
        assertThat(costResult.unitCostSnapshot()).isEqualByComparingTo("0");

        StockOnHand stock = stockRepo.findByBranchId(branchId).get(0);
        assertThat(stock.getQuantity()).isEqualByComparingTo("-50");

        List<StockMovement> movements = movementRepo.findByProductIdAndBranchIdOrderByCreatedAtAsc(productId, branchId);
        assertThat(movements.get(movements.size() - 1).getQuantity()).isEqualByComparingTo("-50");
    }

    @Test
    public void return_createsMovementAndCostLayer() {
        inventoryFacade.recordSaleAndGetCost(productId, branchId, new BigDecimal("10"), UUID.randomUUID().toString(), UUID.randomUUID(), userId);
        
        inventoryFacade.recordReturn(productId, branchId, new BigDecimal("3"), new BigDecimal("8000"), UUID.randomUUID().toString(), UUID.randomUUID(), userId);

        List<StockMovement> movements = movementRepo.findByProductIdAndBranchIdOrderByCreatedAtAsc(productId, branchId);
        // latest is RETURN
        assertThat(movements.get(movements.size() - 1).getMovementType().name()).isEqualTo("RETURN");
        assertThat(movements.get(movements.size() - 1).getQuantity()).isEqualByComparingTo("3");

        List<CostLayer> layers = costLayerRepo.findAvailableForFifoWithLock(productId, branchId);
        assertThat(layers).hasSize(1);
        assertThat(layers.get(0).getCostBasis()).isEqualTo("RETURN");
        assertThat(layers.get(0).getUnitCost()).isEqualByComparingTo("8000");
        assertThat(layers.get(0).getRemainingQty()).isEqualByComparingTo("3");
    }
}
