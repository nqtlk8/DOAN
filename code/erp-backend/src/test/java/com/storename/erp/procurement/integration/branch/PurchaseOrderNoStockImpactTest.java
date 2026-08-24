package com.storename.erp.procurement.integration.branch;

import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.procurement.application.SupplierPurchaseOrderService;
import com.storename.erp.procurement.application.dto.SupplierPurchaseOrderCreateDto;
import com.storename.erp.procurement.domain.Supplier;
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
    "spring.datasource.url=jdbc:h2:mem:branch_proc_test_db2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
public class PurchaseOrderNoStockImpactTest {

    @Autowired
    private SupplierPurchaseOrderService poService;

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private StockOnHandRepository stockRepo;

    private UUID branchId;
    private Supplier supplier;
    private UUID productId;

    @BeforeEach
    void setUp() {
        supplierRepo.deleteAll();
        stockRepo.deleteAll();
        
        branchId = UUID.randomUUID();
        productId = UUID.randomUUID();

        supplier = new Supplier();
        supplier.setSupplierCode("SUP-001");
        supplier.setName("Test Supplier");
        supplier = supplierRepo.save(supplier);
        
        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("100"), "Init");
        stockRepo.save(stock);
    }

    @Test
    void testCreateAndConfirmPO_DoesNotChangeStock() {
        BigDecimal initialStock = getStock(branchId);
        
        SupplierPurchaseOrderCreateDto dto = new SupplierPurchaseOrderCreateDto();
        dto.setSupplierId(supplier.getId());
        dto.setPoCode("PO-TEST-001");
        
        SupplierPurchaseOrderCreateDto.LineDto line = new SupplierPurchaseOrderCreateDto.LineDto();
        line.setProductId(productId);
        line.setQuantity(new BigDecimal("50"));
        line.setUnitCost(new BigDecimal("1500"));
        line.setUnitOfMeasure("PCS");
        dto.setLines(Collections.singletonList(line));

        UUID poId = poService.createDraft(branchId, dto);
        
        assertEquals(0, getStock(branchId).compareTo(initialStock));
        
        poService.confirmOrder(poId, branchId);
        
        assertEquals(0, getStock(branchId).compareTo(initialStock));
    }
    
    private BigDecimal getStock(UUID branchId) {
        return stockRepo.findByProductIdAndBranchId(productId, branchId)
                .map(StockOnHand::getQuantity)
                .orElse(BigDecimal.ZERO);
    }
}
