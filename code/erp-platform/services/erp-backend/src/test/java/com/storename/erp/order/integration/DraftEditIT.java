package com.storename.erp.order.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.common.aop.IdempotencyAspect;
import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import com.storename.erp.common.security.JwtTokenProvider;
import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.domain.CostLayer;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.CostLayerRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.order.application.SalesInvoiceService;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.domain.SalesInvoiceStatus;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sửa chứng từ khi còn nháp (PUT) cho hóa đơn bán, phiếu nhập, phiếu trả.
 *
 * <p>Quy tắc: lưu nháp và sửa nháp KHÔNG tác động kho / công nợ; chỉ bước xác nhận mới tác động, và tác động đúng
 * theo nội dung đã lưu lần cuối. Chứng từ đã xác nhận không sửa được (409 INVALID_STATUS).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"branch", "test"})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_draft_edit_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "instance.role=BRANCH",
    "branch-id=2101"
})
class DraftEditIT {

    private static final long BRANCH = 2101L;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private IdempotencyRecordRepository idempotencyRepo;
    @Autowired private SalesInvoiceRepository invoiceRepo;
    @Autowired private SalesInvoiceService invoiceService;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private StockOnHandRepository stockRepo;
    @Autowired private CostLayerRepository costLayerRepo;
    @Autowired private ReceivableDebtService debtService;
    @Autowired private TransactionTemplate tx;

    private String token;
    private Customer customer;
    private Long productId;
    private Long productId2;

    @BeforeEach
    void setUp() {
        idempotencyRepo.deleteAll();
        token = token(BRANCH);
        productId = (long) (Math.random() * 1_000_000L) + 10_000L;
        productId2 = productId + 1;

        customer = new Customer();
        customer.setCustomerCode("C-EDIT-" + UUID.randomUUID().toString().substring(0, 8));
        customer.setName("Khách kiểm thử sửa nháp");
        customer = customerRepo.save(customer);

        for (Long p : List.of(productId, productId2)) {
            StockOnHand stock = new StockOnHand(p, BRANCH);
            stock.increase(new BigDecimal("100"), "Init");
            stockRepo.save(stock);
            costLayerRepo.save(CostLayer.fromInbound(p, BRANCH, new BigDecimal("100"), new BigDecimal("500"), null));
        }
    }

    private String token(long branch) {
        return "Bearer " + jwtTokenProvider.generateToken(UUID.randomUUID().toString(), "STAFF", String.valueOf(branch),
                UUID.randomUUID().toString());
    }

    private JsonNode json(ResultActions r) throws Exception {
        return objectMapper.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req, String body)
            throws Exception {
        req.header("Authorization", token).contentType(MediaType.APPLICATION_JSON);
        if (body != null) req.content(body);
        return mockMvc.perform(req);
    }

    private BigDecimal stockQty(Long p) {
        return stockRepo.findByProductIdAndBranchId(p, BRANCH).map(StockOnHand::getQuantity).orElse(BigDecimal.ZERO);
    }

    private BigDecimal debt() {
        return debtService.getCurrentDebt(customer.getId(), BRANCH);
    }

    // ------------------------------------------------------------------ hóa đơn bán

    private String invoiceBody(String advance, String... lines) {
        return """
                {"customerId":"%s","note":"ghi chú","advancePayment":%s,"lines":[%s]}
                """.formatted(customer.getId(), advance, String.join(",", lines));
    }

    private String invLine(Long p, String qty, String price) {
        return """
                {"productId":%d,"productName":"SP","quantity":%s,"unitPrice":%s,"unitOfMeasure":"PCS"}"""
                .formatted(p, qty, price);
    }

    private UUID createInvoice(String body) throws Exception {
        return UUID.fromString(json(send(post("/api/v1/sales-invoices"), body).andExpect(status().isCreated()))
                .path("data").path("id").asText());
    }

    @Test
    @DisplayName("Hóa đơn: sửa nháp không đụng kho/công nợ; xác nhận tác động theo nội dung đã sửa")
    void salesInvoice_editDraft_thenConfirm_usesEditedContent() throws Exception {
        UUID id = createInvoice(invoiceBody("0", invLine(productId, "2", "1000"), invLine(productId2, "1", "1000")));
        String code = json(send(get("/api/v1/sales-invoices/" + id), null)).path("data").path("invoiceCode").asText();

        send(put("/api/v1/sales-invoices/" + id), invoiceBody("1000", invLine(productId, "5", "2000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.invoiceCode").value(code));

        // Vẫn là nháp, nội dung đã thay toàn bộ (2 dòng -> 1 dòng), mã không đổi; kho và công nợ chưa đụng tới.
        send(get("/api/v1/sales-invoices/" + id), null)
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.invoiceCode").value(code))
                .andExpect(jsonPath("$.data.totalAmount").value(10000))
                .andExpect(jsonPath("$.data.advancePayment").value(1000))
                .andExpect(jsonPath("$.data.lines.length()").value(1))
                .andExpect(jsonPath("$.data.lines[0].quantity").value(5));
        assertThat(stockQty(productId)).isEqualByComparingTo("100");
        assertThat(stockQty(productId2)).isEqualByComparingTo("100");
        assertThat(debt()).isEqualByComparingTo("0");

        send(post("/api/v1/sales-invoices/" + id + "/confirm"), null).andExpect(status().isOk());

        assertThat(stockQty(productId)).isEqualByComparingTo("95");
        assertThat(stockQty(productId2)).isEqualByComparingTo("100"); // dòng đã xóa khi sửa: không trừ kho
        assertThat(debt()).isEqualByComparingTo("9000"); // 10.000 - trả trước 1.000
        send(get("/api/v1/sales-invoices/" + id), null)
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.remainingDebt").value(9000));
    }

    @Test
    @DisplayName("Hóa đơn đã xác nhận: sửa -> 409 INVALID_STATUS, nội dung/kho/công nợ giữ nguyên")
    void salesInvoice_editConfirmed_isRejected() throws Exception {
        UUID id = createInvoice(invoiceBody("0", invLine(productId, "2", "1000")));
        send(post("/api/v1/sales-invoices/" + id + "/confirm"), null).andExpect(status().isOk());

        send(put("/api/v1/sales-invoices/" + id), invoiceBody("0", invLine(productId, "50", "1000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(SalesInvoice.INVALID_STATUS));

        send(get("/api/v1/sales-invoices/" + id), null)
                .andExpect(jsonPath("$.data.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(2000));
        assertThat(stockQty(productId)).isEqualByComparingTo("98");
        assertThat(debt()).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("Hóa đơn: sửa với dữ liệu sai -> 400, nháp giữ nguyên")
    void salesInvoice_invalidEdit_keepsDraft() throws Exception {
        UUID id = createInvoice(invoiceBody("0", invLine(productId, "2", "1000")));

        send(put("/api/v1/sales-invoices/" + id), invoiceBody("0")).andExpect(status().isBadRequest()); // không có dòng
        send(put("/api/v1/sales-invoices/" + id), invoiceBody("99999", invLine(productId, "1", "1000")))
                .andExpect(status().isBadRequest()); // trả trước > tổng tiền

        send(get("/api/v1/sales-invoices/" + id), null)
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.lines[0].quantity").value(2));
    }

    @Test
    @DisplayName("Hóa đơn: chi nhánh khác không sửa được (403); không tồn tại -> 404")
    void salesInvoice_otherBranch_forbidden() throws Exception {
        UUID id = createInvoice(invoiceBody("0", invLine(productId, "2", "1000")));

        mockMvc.perform(put("/api/v1/sales-invoices/" + id)
                        .header("Authorization", token(BRANCH + 1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceBody("0", invLine(productId, "9", "1000"))))
                .andExpect(status().isForbidden());
        send(put("/api/v1/sales-invoices/" + UUID.randomUUID()), invoiceBody("0", invLine(productId, "1", "1000")))
                .andExpect(status().isNotFound());

        send(get("/api/v1/sales-invoices/" + id), null).andExpect(jsonPath("$.data.lines[0].quantity").value(2));
    }

    @Test
    @DisplayName("Hóa đơn: Lưu lại cùng key, cùng nội dung -> chạy một lần; cùng key khác nội dung -> 409 KEY_REUSED")
    void salesInvoice_editIdempotency() throws Exception {
        UUID id = createInvoice(invoiceBody("0", invLine(productId, "2", "1000")));
        String key = "sales-invoice-update:" + id + ":" + UUID.randomUUID();
        String body = invoiceBody("0", invLine(productId, "3", "1000"));

        send(put("/api/v1/sales-invoices/" + id).header("Idempotency-Key", key), body).andExpect(status().isOk());
        send(put("/api/v1/sales-invoices/" + id).header("Idempotency-Key", key), body).andExpect(status().isOk());
        send(put("/api/v1/sales-invoices/" + id).header("Idempotency-Key", key), invoiceBody("0", invLine(productId, "4", "1000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(IdempotencyAspect.KEY_REUSED));

        send(get("/api/v1/sales-invoices/" + id), null).andExpect(jsonPath("$.data.lines[0].quantity").value(3));
    }

    /**
     * Sửa và xác nhận chạy đồng thời nhiều lần. Bản sửa chỉ đổi sản phẩm (cùng số lượng, cùng đơn giá) nên đầu hóa
     * đơn không đổi và {@code @Version} không bắt được xung đột; chỉ khóa hóa đơn (FOR UPDATE) mới giữ được tính
     * nhất quán: kho bị trừ phải đúng sản phẩm trên hóa đơn đã xác nhận.
     */
    @Test
    @DisplayName("Sửa và xác nhận đồng thời: kho bị trừ luôn khớp nội dung hóa đơn đã xác nhận")
    void salesInvoice_concurrentEditAndConfirm_consistent() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 10; round++) {
                BigDecimal before1 = stockQty(productId);
                BigDecimal before2 = stockQty(productId2);
                UUID id = invoiceService.createDraft(dto(productId, "2"), BRANCH).getId();
                CountDownLatch start = new CountDownLatch(1);

                Future<?> edit = pool.submit(() -> {
                    start.await();
                    try {
                        invoiceService.updateDraft(id, dto(productId2, "2"), BRANCH);
                    } catch (com.storename.erp.common.exception.BusinessConflictException e) {
                        // xác nhận chạy trước -> sửa bị từ chối: hợp lệ
                    }
                    return null;
                });
                Future<?> confirm = pool.submit(() -> {
                    start.await();
                    invoiceService.confirmInvoice(id, BRANCH, UUID.randomUUID());
                    return null;
                });
                start.countDown();
                edit.get(30, TimeUnit.SECONDS);
                confirm.get(30, TimeUnit.SECONDS);

                java.util.Map<Long, BigDecimal> onInvoice = tx.execute(s -> {
                    SalesInvoice inv = invoiceRepo.findById(id).orElseThrow();
                    assertThat(inv.getStatus()).isEqualTo(SalesInvoiceStatus.CONFIRMED);
                    java.util.Map<Long, BigDecimal> m = new java.util.HashMap<>();
                    inv.getLines().forEach(l -> m.merge(l.getProductId(), l.getQuantity(), BigDecimal::add));
                    return m;
                });
                assertThat(before1.subtract(stockQty(productId)))
                        .as("round %d: kho SP1 bị trừ phải bằng SL SP1 trên hóa đơn đã xác nhận", round)
                        .isEqualByComparingTo(onInvoice.getOrDefault(productId, BigDecimal.ZERO));
                assertThat(before2.subtract(stockQty(productId2)))
                        .as("round %d: kho SP2 bị trừ phải bằng SL SP2 trên hóa đơn đã xác nhận", round)
                        .isEqualByComparingTo(onInvoice.getOrDefault(productId2, BigDecimal.ZERO));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private SalesInvoiceCreateDto dto(Long product, String qty) {
        SalesInvoiceCreateDto d = new SalesInvoiceCreateDto();
        d.setCustomerId(customer.getId());
        d.setAdvancePayment(BigDecimal.ZERO);
        com.storename.erp.order.application.dto.SalesInvoiceLineDto l = new com.storename.erp.order.application.dto.SalesInvoiceLineDto();
        l.setProductId(product);
        l.setProductName("SP");
        l.setQuantity(new BigDecimal(qty));
        l.setUnitPrice(new BigDecimal("1000"));
        l.setUnitOfMeasure("PCS");
        d.setLines(List.of(l));
        return d;
    }

    // ------------------------------------------------------------------ phiếu nhập

    private String inboundBody(String note, String qty, String cost) {
        return """
                {"note":"%s","lines":[{"productId":%d,"quantity":%s,"unitCost":%s,"unitOfMeasure":"PCS"}]}
                """.formatted(note, productId, qty, cost);
    }

    @Test
    @DisplayName("Phiếu nhập: sửa nháp chưa nhập kho; xác nhận nhập đúng số đã sửa; đã xác nhận -> 409")
    void inbound_editDraft_thenConfirm() throws Exception {
        UUID id = UUID.fromString(json(send(post("/api/v1/inventory/inbound"), inboundBody("lần 1", "10", "500"))
                .andExpect(status().isCreated())).path("data").path("id").asText());

        send(put("/api/v1/inventory/inbound/" + id), inboundBody("lần 2", "20", "600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()));
        send(get("/api/v1/inventory/inbound/" + id), null)
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.note").value("lần 2"))
                .andExpect(jsonPath("$.data.lines.length()").value(1))
                .andExpect(jsonPath("$.data.lines[0].quantity").value(20))
                .andExpect(jsonPath("$.data.lines[0].unitCost").value(600));
        assertThat(stockQty(productId)).isEqualByComparingTo("100");

        send(post("/api/v1/inventory/inbound/" + id + "/confirm"), null).andExpect(status().isOk());
        assertThat(stockQty(productId)).isEqualByComparingTo("120");

        send(put("/api/v1/inventory/inbound/" + id), inboundBody("lần 3", "99", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_STATUS"));
        send(get("/api/v1/inventory/inbound/" + id), null).andExpect(jsonPath("$.data.lines[0].quantity").value(20));
        assertThat(stockQty(productId)).isEqualByComparingTo("120");
    }

    // ------------------------------------------------------------------ phiếu trả

    private String returnBody(UUID invoiceId, String qty) {
        return """
                {"customerId":"%s","invoiceId":"%s","reason":"lỗi","note":"ghi chú trả",
                 "lines":[{"productId":%d,"quantity":%s,"unitPrice":1000,"unitOfMeasure":"PCS"}]}
                """.formatted(customer.getId(), invoiceId, productId, qty);
    }

    @Test
    @DisplayName("Phiếu trả: sửa nháp kiểm tra lại SL theo hóa đơn gốc; xác nhận theo nội dung đã sửa; đã xác nhận -> 409")
    void goodsReturn_editDraft_thenConfirm() throws Exception {
        UUID invoiceId = createInvoice(invoiceBody("0", invLine(productId, "5", "1000")));
        send(post("/api/v1/sales-invoices/" + invoiceId + "/confirm"), null).andExpect(status().isOk());
        assertThat(stockQty(productId)).isEqualByComparingTo("95");
        assertThat(debt()).isEqualByComparingTo("5000");

        UUID id = UUID.fromString(json(send(post("/api/v1/goods-returns"), returnBody(invoiceId, "2"))
                .andExpect(status().isCreated())).path("data").asText());

        // Vượt số đã bán (5) -> 400, nháp giữ nguyên.
        send(put("/api/v1/goods-returns/" + id), returnBody(invoiceId, "6")).andExpect(status().isBadRequest());
        send(get("/api/v1/goods-returns/" + id), null).andExpect(jsonPath("$.data.lines[0].quantity").value(2));

        send(put("/api/v1/goods-returns/" + id), returnBody(invoiceId, "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(id.toString()));
        send(get("/api/v1/goods-returns/" + id), null)
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.note").value("ghi chú trả"))
                .andExpect(jsonPath("$.data.invoiceId").value(invoiceId.toString()))
                .andExpect(jsonPath("$.data.totalAmount").value(3000))
                .andExpect(jsonPath("$.data.lines[0].quantity").value(3));
        assertThat(stockQty(productId)).isEqualByComparingTo("95");
        assertThat(debt()).isEqualByComparingTo("5000");

        send(post("/api/v1/goods-returns/" + id + "/confirm"), null).andExpect(status().isOk());
        assertThat(stockQty(productId)).isEqualByComparingTo("98");
        assertThat(debt()).isEqualByComparingTo("2000");

        send(put("/api/v1/goods-returns/" + id), returnBody(invoiceId, "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(SalesInvoice.INVALID_STATUS));
        assertThat(stockQty(productId)).isEqualByComparingTo("98");
    }
}
