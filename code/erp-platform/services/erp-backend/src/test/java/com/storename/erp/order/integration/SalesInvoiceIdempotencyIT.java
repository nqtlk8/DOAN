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
import com.storename.erp.order.domain.SalesInvoice;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chống tạo trùng hóa đơn bán ở mức API: key theo lần lập phiếu, request_key trên chứng từ,
 * xác nhận hai bước. Kịch bản bám theo lỗi thực tế: client hết thời gian chờ rồi bấm Lưu lại.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"branch", "test"})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_sales_idem_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "instance.role=BRANCH",
    "branch-id=2001"
})
class SalesInvoiceIdempotencyIT {

    private static final long BRANCH = 2001L;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private IdempotencyRecordRepository idempotencyRepo;
    @Autowired private SalesInvoiceRepository invoiceRepo;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private StockOnHandRepository stockRepo;
    @Autowired private CostLayerRepository costLayerRepo;
    @Autowired private ReceivableDebtService debtService;

    private String staffToken;
    private Customer customer;
    private Long productId;

    @BeforeEach
    void setUp() {
        idempotencyRepo.deleteAll();
        staffToken = token(UUID.randomUUID());
        productId = (long) (Math.random() * 1_000_000L) + 10_000L;

        customer = new Customer();
        customer.setCustomerCode("C-IDEM-" + UUID.randomUUID().toString().substring(0, 8));
        customer.setName("Khách kiểm thử idempotency");
        customer = customerRepo.save(customer);

        StockOnHand stock = new StockOnHand(productId, BRANCH);
        stock.increase(new BigDecimal("100"), "Init");
        stockRepo.save(stock);
        costLayerRepo.save(CostLayer.fromInbound(productId, BRANCH, new BigDecimal("100"), new BigDecimal("500"), null));
    }

    private String token(UUID subject) {
        return "Bearer " + jwtTokenProvider.generateToken(subject.toString(), "STAFF", String.valueOf(BRANCH), UUID.randomUUID().toString());
    }

    private String body(String quantity) {
        return """
                {"customerId":"%s","note":"","advancePayment":0,
                 "lines":[{"productId":%d,"productName":"SP","quantity":%s,"unitPrice":1000,"unitOfMeasure":"PCS"}]}
                """.formatted(customer.getId(), productId, quantity);
    }

    private MockHttpServletRequestBuilder createRequest(String token, String key, String body) {
        MockHttpServletRequestBuilder req = post("/api/v1/sales-invoices")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        return key == null ? req : req.header("Idempotency-Key", key);
    }

    private MockHttpServletRequestBuilder confirmRequest(String token, String key, UUID invoiceId) {
        MockHttpServletRequestBuilder req = post("/api/v1/sales-invoices/" + invoiceId + "/confirm")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);
        return key == null ? req : req.header("Idempotency-Key", key);
    }

    private UUID invoiceId(ResultActions result) throws Exception {
        JsonNode json = objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
        return UUID.fromString(json.path("data").path("id").asText());
    }

    private List<SalesInvoice> invoicesOfCustomer() {
        return invoiceRepo.findByBranchId(BRANCH).stream()
                .filter(i -> i.getCustomerId().equals(customer.getId()))
                .toList();
    }

    private BigDecimal stockQty() {
        return stockRepo.findByProductIdAndBranchId(productId, BRANCH).orElseThrow().getQuantity();
    }

    @Test
    @DisplayName("Bấm Lưu lại với cùng key (vd sau khi hết thời gian chờ): trả đúng hóa đơn cũ, không tạo thêm")
    void sameKey_twice_returnsSameDraft() throws Exception {
        String key = UUID.randomUUID().toString();

        UUID first = invoiceId(mockMvc.perform(createRequest(staffToken, key, body("2"))).andExpect(status().isCreated()));
        UUID second = invoiceId(mockMvc.perform(createRequest(staffToken, key, body("2"))).andExpect(status().isCreated()));

        assertThat(second).isEqualTo(first);
        assertThat(invoicesOfCustomer()).hasSize(1);
        SalesInvoice saved = invoiceRepo.findById(first).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(SalesInvoiceStatus.DRAFT);
        assertThat(saved.getRequestKey()).endsWith(":" + key);
        assertThat(stockQty()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("Cùng key nhưng nội dung khác -> 409 IDEMPOTENCY_KEY_REUSED, không tạo hóa đơn mới")
    void sameKey_differentBody_isRejected() throws Exception {
        String key = UUID.randomUUID().toString();
        mockMvc.perform(createRequest(staffToken, key, body("2"))).andExpect(status().isCreated());

        mockMvc.perform(createRequest(staffToken, key, body("3")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(IdempotencyAspect.KEY_REUSED));

        assertThat(invoicesOfCustomer()).hasSize(1);
    }

    @Test
    @DisplayName("Mất bản ghi idempotency (máy chủ dừng giữa chừng / đã dọn): request_key vẫn chặn tạo trùng")
    void idempotencyRecordLost_requestKeyStillPreventsDuplicate() throws Exception {
        String key = UUID.randomUUID().toString();
        UUID first = invoiceId(mockMvc.perform(createRequest(staffToken, key, body("2"))).andExpect(status().isCreated()));

        idempotencyRepo.deleteAll();
        UUID second = invoiceId(mockMvc.perform(createRequest(staffToken, key, body("2"))).andExpect(status().isCreated()));

        assertThat(second).isEqualTo(first);
        assertThat(invoicesOfCustomer()).hasSize(1);
    }

    @Test
    @DisplayName("Key mới cho mỗi lần lập phiếu: hai hóa đơn giống hệt nhau vẫn được tạo đủ hai")
    void differentKeys_sameBody_createTwoInvoices() throws Exception {
        UUID first = invoiceId(mockMvc.perform(createRequest(staffToken, UUID.randomUUID().toString(), body("2"))).andExpect(status().isCreated()));
        UUID second = invoiceId(mockMvc.perform(createRequest(staffToken, UUID.randomUUID().toString(), body("2"))).andExpect(status().isCreated()));

        assertThat(second).isNotEqualTo(first);
        assertThat(invoicesOfCustomer()).hasSize(2);
    }

    @Test
    @DisplayName("Người dùng khác gửi trùng header: không nhận phản hồi đã lưu của người trước")
    void sameHeaderKey_differentUsers_areIndependent() throws Exception {
        String key = UUID.randomUUID().toString();
        UUID first = invoiceId(mockMvc.perform(createRequest(staffToken, key, body("2"))).andExpect(status().isCreated()));
        UUID second = invoiceId(mockMvc.perform(createRequest(token(UUID.randomUUID()), key, body("2"))).andExpect(status().isCreated()));

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    @DisplayName("Xác nhận gửi lại cùng key: trừ kho và ghi công nợ đúng một lần, cả hai lần đều 200")
    void confirm_sameKey_twice_appliesOnce() throws Exception {
        UUID id = invoiceId(mockMvc.perform(createRequest(staffToken, UUID.randomUUID().toString(), body("5"))).andExpect(status().isCreated()));
        String confirmKey = "sales-invoice-confirm:" + id;

        mockMvc.perform(confirmRequest(staffToken, confirmKey, id)).andExpect(status().isOk());
        mockMvc.perform(confirmRequest(staffToken, confirmKey, id)).andExpect(status().isOk());

        assertThat(invoiceRepo.findById(id).orElseThrow().getStatus()).isEqualTo(SalesInvoiceStatus.CONFIRMED);
        assertThat(stockQty()).isEqualByComparingTo("95");
        assertThat(debtService.getCurrentDebt(customer.getId(), BRANCH)).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("Xác nhận lần hai không kèm key -> 409 INVALID_STATUS, kho không bị trừ thêm")
    void confirm_twice_withoutKey_secondIsInvalidStatus() throws Exception {
        UUID id = invoiceId(mockMvc.perform(createRequest(staffToken, null, body("5"))).andExpect(status().isCreated()));

        mockMvc.perform(confirmRequest(staffToken, null, id)).andExpect(status().isOk());
        mockMvc.perform(confirmRequest(staffToken, null, id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(SalesInvoice.INVALID_STATUS));

        assertThat(stockQty()).isEqualByComparingTo("95");
    }

    @Test
    @DisplayName("Xác nhận vượt tồn -> 409, hóa đơn vẫn là nháp; key được giải phóng để gửi lại sau khi nhập hàng")
    void confirm_insufficientStock_keepsDraft_andKeyReusable() throws Exception {
        UUID id = invoiceId(mockMvc.perform(createRequest(staffToken, UUID.randomUUID().toString(), body("150"))).andExpect(status().isCreated()));
        String confirmKey = "sales-invoice-confirm:" + id;

        mockMvc.perform(confirmRequest(staffToken, confirmKey, id)).andExpect(status().isConflict());
        assertThat(invoiceRepo.findById(id).orElseThrow().getStatus()).isEqualTo(SalesInvoiceStatus.DRAFT);

        // Nhập thêm hàng rồi xác nhận lại với CÙNG key: lần lỗi trước đã xóa key nên lần này chạy thật.
        StockOnHand stock = stockRepo.findByProductIdAndBranchId(productId, BRANCH).orElseThrow();
        stock.increase(new BigDecimal("100"), "Top-up");
        stockRepo.save(stock);
        costLayerRepo.save(CostLayer.fromInbound(productId, BRANCH, new BigDecimal("100"), new BigDecimal("600"), null));

        mockMvc.perform(confirmRequest(staffToken, confirmKey, id)).andExpect(status().isOk());
        assertThat(invoiceRepo.findById(id).orElseThrow().getStatus()).isEqualTo(SalesInvoiceStatus.CONFIRMED);
        assertThat(stockQty()).isEqualByComparingTo("50");
    }
}
