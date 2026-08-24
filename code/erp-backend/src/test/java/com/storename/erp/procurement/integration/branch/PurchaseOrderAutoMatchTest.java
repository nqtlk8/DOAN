package com.storename.erp.procurement.integration.branch;

import com.storename.erp.inventory.application.InboundReceiptService;
import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.procurement.application.PayableDebtService;
import com.storename.erp.procurement.application.SupplierPurchaseOrderService;
import com.storename.erp.procurement.application.dto.SupplierPurchaseOrderCreateDto;
import com.storename.erp.procurement.domain.PurchaseOrderStatus;
import com.storename.erp.procurement.domain.Supplier;
import com.storename.erp.procurement.domain.SupplierPurchaseOrder;
import com.storename.erp.procurement.infrastructure.SupplierPurchaseOrderRepository;
import com.storename.erp.procurement.infrastructure.SupplierRepository;
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
    "spring.datasource.url=jdbc:h2:mem:branch_proc_test_db3;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
public class PurchaseOrderAutoMatchTest {

    @Autowired
    private SupplierPurchaseOrderService poService;

    @Autowired
    private SupplierPurchaseOrderRepository poRepo;

    @Autowired
    private InboundReceiptService inboundService;

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private StockOnHandRepository stockRepo;
    
    @Autowired
    private PayableDebtService debtService;

    private UUID branchId;
    private Supplier supplier;
    private UUID productId;

    @BeforeEach
    void setUp() {
        supplierRepo.deleteAll();
        stockRepo.deleteAll();
        poRepo.deleteAll();
        
        branchId = UUID.randomUUID();
        productId = UUID.randomUUID();

        supplier = new Supplier();
        supplier.setSupplierCode("SUP-002");
        supplier.setName("Test Supplier 2");
        supplier = supplierRepo.save(supplier);
        
        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("10"), "Init");
        stockRepo.save(stock);
    }

    @Test
    void testInboundReceiptConfirm_AutoReceivesPO_AndIncreasesDebt() {
        SupplierPurchaseOrderCreateDto poDto = new SupplierPurchaseOrderCreateDto();
        poDto.setSupplierId(supplier.getId());
        poDto.setPoCode("PO-TEST-002");
        
        SupplierPurchaseOrderCreateDto.LineDto poLine = new SupplierPurchaseOrderCreateDto.LineDto();
        poLine.setProductId(productId);
        poLine.setQuantity(new BigDecimal("50"));
        poLine.setUnitCost(new BigDecimal("2000"));
        poLine.setUnitOfMeasure("PCS");
        poDto.setLines(Collections.singletonList(poLine));

        UUID poId = poService.createDraft(branchId, poDto);
        poService.confirmOrder(poId, branchId);
        
        assertEquals(0, debtService.getCurrentDebt(supplier.getId(), branchId).compareTo(BigDecimal.ZERO));
        
        InboundReceiptCreateDto inDto = new InboundReceiptCreateDto();
        inDto.setReceiptCode("IN-TEST-002");
        inDto.setPurchaseOrderId(poId);
        
        InboundReceiptCreateDto.LineDto inLine = new InboundReceiptCreateDto.LineDto();
        inLine.setProductId(productId);
        inLine.setQuantity(new BigDecimal("50"));
        inLine.setUnitCost(new BigDecimal("2000"));
        inLine.setUnitOfMeasure("PCS");
        inDto.setLines(Collections.singletonList(inLine));
        
        UUID receiptId = inboundService.createDraft(branchId, inDto);
        
        // Confirm inbound receipt (should trigger event to auto-receive PO)
        inboundService.confirmReceipt(receiptId, branchId, UUID.randomUUID());
        
        SupplierPurchaseOrder updatedPo = poRepo.findById(poId).orElseThrow();
        assertEquals(PurchaseOrderStatus.RECEIVED, updatedPo.getStatus());
        
        // Debt should increase by 50 * 2000 = 100000
        BigDecimal debtAfter = debtService.getCurrentDebt(supplier.getId(), branchId);
        assertEquals(0, debtAfter.compareTo(new BigDecimal("100000")));
        
        // Stock should increase by 50
        BigDecimal stockAfter = stockRepo.findByProductIdAndBranchId(productId, branchId).get().getQuantity();
        assertEquals(0, stockAfter.compareTo(new BigDecimal("60"))); // 10 + 50
    }
}
