package com.storename.erp.order.integration;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.order.application.SalesInvoiceService;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceLineDto;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceStatus;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("branch")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_order_test_db2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
public class SalesInvoiceIT {

    @Autowired
    private SalesInvoiceService invoiceService;

    @Autowired
    private ReceivableDebtService debtService;

    @Autowired
    private CustomerRepository customerRepo;
    
    @Autowired
    private StockOnHandRepository stockRepo;
    
    @Autowired
    private SalesInvoiceRepository invoiceRepo;

    @Autowired
    private com.storename.erp.inventory.infrastructure.CostLayerRepository costLayerRepo;

    private Long branchId;
    private Customer customer;
    private Long productId;

    @Autowired
    private com.storename.erp.crm.infrastructure.ReceivableDebtRepository debtRepo;

    @Autowired
    private com.storename.erp.crm.infrastructure.ReceivableDebtMovementRepository movementRepo;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM receivable_debt_movement");
        debtRepo.deleteAll();
        customerRepo.deleteAll();
        stockRepo.deleteAll();
        invoiceRepo.deleteAll();
        
        branchId = (long)(Math.random() * 100000L);
        productId = (long)(Math.random() * 100000L);

        customer = new Customer();
        customer.setCustomerCode("CUST-INV-01");
        customer.setName("Test Customer");
        customer = customerRepo.save(customer);

        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("100"), "Init");
        stockRepo.save(stock);
        // Tồn kho phải đi kèm lớp giá vốn (như phiếu nhập thật) — FIFO không cho xuất khi thiếu lớp.
        costLayerRepo.save(com.storename.erp.inventory.domain.CostLayer.fromInbound(productId, branchId, new BigDecimal("100"), new BigDecimal("500"), null));
    }

    private SalesInvoiceCreateDto invoiceDto(String code, Long product, String qty, String price) {
        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setInvoiceCode(code);
        dto.setCustomerId(customer.getId());
        SalesInvoiceLineDto line = new SalesInvoiceLineDto();
        line.setProductId(product);
        line.setProductName("Test Product");
        line.setQuantity(new BigDecimal(qty));
        line.setUnitPrice(new BigDecimal(price));
        line.setUnitOfMeasure("PCS");
        dto.setLines(Collections.singletonList(line));
        return dto;
    }

    private SalesInvoice createAndConfirm(SalesInvoiceCreateDto dto) {
        SalesInvoice draft = invoiceService.createDraft(dto, branchId);
        return invoiceService.confirmInvoice(draft.getId(), branchId, UUID.randomUUID());
    }

    private BigDecimal stockQty() {
        return stockRepo.findByProductIdAndBranchId(productId, branchId).orElseThrow().getQuantity();
    }

    @Test
    void testDebtSnapshotImmutability() {
        SalesInvoice confirmed = createAndConfirm(invoiceDto("INV-TEST-002", productId, "2", "1000"));

        assertEquals(0, confirmed.getPreviousDebt().compareTo(BigDecimal.ZERO));
        assertEquals(0, confirmed.getRemainingDebt().compareTo(new BigDecimal("2000")));

        // Modify debt outside of invoice
        debtService.increaseDebt(customer.getId(), branchId, new BigDecimal("5000"),
                com.storename.erp.crm.domain.ReceivableDebtMovementType.ADJUSTMENT, "TEST", "TEST", UUID.randomUUID(), "test");

        // Refetch invoice
        SalesInvoice refetched = invoiceRepo.findById(confirmed.getId()).orElseThrow();

        // Snapshot should remain unchanged
        assertEquals(0, refetched.getPreviousDebt().compareTo(BigDecimal.ZERO));
        assertEquals(0, refetched.getRemainingDebt().compareTo(new BigDecimal("2000")));
    }

    @Test
    void draft_doesNotTouchStockOrDebt() {
        SalesInvoice draft = invoiceService.createDraft(invoiceDto("INV-DRAFT-01", productId, "5", "1000"), branchId);

        assertEquals(SalesInvoiceStatus.DRAFT, invoiceRepo.findById(draft.getId()).orElseThrow().getStatus());
        assertEquals(0, stockQty().compareTo(new BigDecimal("100")), "lưu nháp không trừ kho");
        assertEquals(0, debtService.getCurrentDebt(customer.getId(), branchId).compareTo(BigDecimal.ZERO),
                "lưu nháp không ghi công nợ");
    }

    @Test
    void confirm_deductsStockAndRaisesDebt() {
        SalesInvoice draft = invoiceService.createDraft(invoiceDto("INV-CONF-01", productId, "5", "1000"), branchId);

        SalesInvoice confirmed = invoiceService.confirmInvoice(draft.getId(), branchId, UUID.randomUUID());

        assertEquals(SalesInvoiceStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(0, stockQty().compareTo(new BigDecimal("95")));
        assertEquals(0, debtService.getCurrentDebt(customer.getId(), branchId).compareTo(new BigDecimal("5000")));
        assertEquals(0, confirmed.getLines().get(0).getUnitCost().compareTo(new BigDecimal("500")), "giá vốn FIFO được chụp khi xác nhận");
    }

    @Test
    void confirmTwice_secondIsRejected_effectsAppliedOnce() {
        SalesInvoice draft = invoiceService.createDraft(invoiceDto("INV-CONF-02", productId, "5", "1000"), branchId);
        invoiceService.confirmInvoice(draft.getId(), branchId, UUID.randomUUID());

        com.storename.erp.common.exception.BusinessConflictException ex = assertThrows(
                com.storename.erp.common.exception.BusinessConflictException.class,
                () -> invoiceService.confirmInvoice(draft.getId(), branchId, UUID.randomUUID()));
        assertEquals(SalesInvoice.INVALID_STATUS, ex.getCode());
        assertEquals(0, stockQty().compareTo(new BigDecimal("95")), "kho chỉ bị trừ một lần");
        assertEquals(0, debtService.getCurrentDebt(customer.getId(), branchId).compareTo(new BigDecimal("5000")),
                "công nợ chỉ tăng một lần");
    }

    @Test
    void confirmFromAnotherBranch_isForbidden() {
        SalesInvoice draft = invoiceService.createDraft(invoiceDto("INV-CONF-03", productId, "1", "1000"), branchId);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> invoiceService.confirmInvoice(draft.getId(), branchId + 1, UUID.randomUUID()));
        assertEquals(SalesInvoiceStatus.DRAFT, invoiceRepo.findById(draft.getId()).orElseThrow().getStatus());
    }

    @Test
    void createDraft_sameRequestKey_returnsExistingInvoice() {
        String key = "user-1:" + UUID.randomUUID();
        SalesInvoice first = invoiceService.createDraft(invoiceDto(null, productId, "1", "1000"), branchId, key);
        SalesInvoice second = invoiceService.createDraft(invoiceDto(null, productId, "1", "1000"), branchId, key);

        assertEquals(first.getId(), second.getId());
        assertEquals(1, invoiceRepo.count(), "cùng khóa lập phiếu chỉ tạo một hóa đơn");
        assertEquals(key, invoiceRepo.findById(first.getId()).orElseThrow().getRequestKey());
    }

    @Test
    void createDraft_duplicateInvoiceCode_isRejected() {
        invoiceService.createDraft(invoiceDto("INV-DUP-01", productId, "1", "1000"), branchId);

        assertThrows(IllegalArgumentException.class,
                () -> invoiceService.createDraft(invoiceDto("INV-DUP-01", productId, "1", "1000"), branchId));
    }

    @Test
    void confirmFailure_rollsBack_invoiceStaysDraft() {
        // Sản phẩm không có tồn/lớp giá vốn: bước trừ kho ném lỗi khi xác nhận.
        Long unknownProductId = productId + 1_000_000L;
        SalesInvoice draft = invoiceService.createDraft(invoiceDto("INV-TEST-003", unknownProductId, "2", "1000"), branchId);

        assertThrows(RuntimeException.class, () -> invoiceService.confirmInvoice(draft.getId(), branchId, UUID.randomUUID()));

        // Xác nhận rollback toàn bộ: hóa đơn vẫn là DRAFT, công nợ không đổi.
        assertEquals(SalesInvoiceStatus.DRAFT, invoiceRepo.findById(draft.getId()).orElseThrow().getStatus());
        assertEquals(0, debtService.getCurrentDebt(customer.getId(), branchId).compareTo(BigDecimal.ZERO));
    }

    @Test
    void confirm_exceedsStock_isRejectedAndRolledBack() {
        SalesInvoice draft = invoiceService.createDraft(invoiceDto("INV-TEST-EXCEED", productId, "150", "1000"), branchId);

        // Không cho tồn âm: xác nhận bị từ chối, toàn bộ giao dịch rollback
        com.storename.erp.inventory.domain.StockInsufficientException ex = assertThrows(
                com.storename.erp.inventory.domain.StockInsufficientException.class,
                () -> invoiceService.confirmInvoice(draft.getId(), branchId, UUID.randomUUID()));
        assertTrue(ex.getMessage().contains("tồn kho còn 100, cần 150"), ex.getMessage());

        assertEquals(0, stockQty().compareTo(new BigDecimal("100")));
        assertEquals(SalesInvoiceStatus.DRAFT, invoiceRepo.findById(draft.getId()).orElseThrow().getStatus(),
                "hóa đơn vẫn là nháp để sửa hoặc nhập thêm hàng");
        assertEquals(0, debtService.getCurrentDebt(customer.getId(), branchId).compareTo(BigDecimal.ZERO));
    }

    @Test
    void createAndConfirm_multipleProducts() {
        Long productId2 = (long)(Math.random() * 100000L);
        StockOnHand stock2 = new StockOnHand(productId2, branchId);
        stock2.increase(new BigDecimal("50"), "Init");
        stockRepo.save(stock2);
        costLayerRepo.save(com.storename.erp.inventory.domain.CostLayer.fromInbound(productId2, branchId, new BigDecimal("50"), new BigDecimal("800"), null));

        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setInvoiceCode("INV-TEST-MULTI");
        dto.setCustomerId(customer.getId());

        SalesInvoiceLineDto line1 = new SalesInvoiceLineDto();
        line1.setProductId(productId);
        line1.setProductName("Test Product 1");
        line1.setQuantity(new BigDecimal("30"));
        line1.setUnitPrice(new BigDecimal("1000"));
        line1.setUnitOfMeasure("PCS");

        SalesInvoiceLineDto line2 = new SalesInvoiceLineDto();
        line2.setProductId(productId2);
        line2.setProductName("Test Product 2");
        line2.setQuantity(new BigDecimal("20"));
        line2.setUnitPrice(new BigDecimal("2000"));
        line2.setUnitOfMeasure("PCS");

        dto.setLines(java.util.Arrays.asList(line1, line2));

        createAndConfirm(dto);

        assertEquals(70, stockRepo.findByProductIdAndBranchId(productId, branchId).get().getQuantity().intValue());
        assertEquals(30, stockRepo.findByProductIdAndBranchId(productId2, branchId).get().getQuantity().intValue());
    }
}
