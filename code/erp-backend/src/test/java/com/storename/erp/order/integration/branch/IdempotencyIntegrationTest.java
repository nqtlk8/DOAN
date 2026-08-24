package com.storename.erp.order.integration.branch;

import com.storename.erp.common.domain.IdempotencyRecord;
import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.order.api.SalesInvoiceController;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceLineDto;
import com.storename.erp.order.domain.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    "spring.datasource.url=jdbc:h2:mem:branch_order_test_db3;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.security.user.name=user",
    "spring.security.user.password=password"
})
public class IdempotencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IdempotencyRecordRepository idempotencyRepo;

    @Autowired
    private CustomerRepository customerRepo;
    
    @Autowired
    private StockOnHandRepository stockRepo;

    private UUID branchId;
    private UUID customerId;
    private UUID productId;

    @Autowired
    private com.storename.erp.order.infrastructure.SalesInvoiceRepository invoiceRepo;
    
    @Autowired
    private com.storename.erp.inventory.application.InboundReceiptService inboundService;
    
    @Autowired
    private com.storename.erp.order.application.SalesInvoiceService salesInvoiceService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    
    @Autowired
    private com.storename.erp.common.security.JwtTokenProvider jwtTokenProvider;
    
    private String authToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        idempotencyRepo.deleteAll();
        invoiceRepo.deleteAll();
        stockRepo.deleteAll();
        customerRepo.deleteAll();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        
        branchId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        
        authToken = "Bearer " + jwtTokenProvider.generateToken("testUser", "ADMIN", branchId.toString(), UUID.randomUUID().toString());

        Customer customer = new Customer();
        customer.setCustomerCode("CUST-" + UUID.randomUUID().toString().substring(0,5));
        customer.setName("Test Customer");
        customer.setCustomerType(com.storename.erp.crm.domain.CustomerType.RETAIL);
        customer = customerRepo.save(customer);
        customerId = customer.getId();
        
        StockOnHand stock = new StockOnHand(productId, branchId);
        stock = stockRepo.save(stock);
        stock.increase(BigDecimal.valueOf(100), "INIT");
        stockRepo.save(stock);
    }
    
    private String createToken() {
        return "Bearer dummy-token";
    }
    
    private UUID createDraftInvoice() {
        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setCustomerId(customerId);
        dto.setInvoiceCode("INV-" + UUID.randomUUID().toString().substring(0,5));
        dto.setPaymentMethod(PaymentMethod.CASH);
        
        SalesInvoiceLineDto line = new SalesInvoiceLineDto();
        line.setProductId(productId);
        line.setQuantity(BigDecimal.valueOf(10));
        line.setUnitPrice(BigDecimal.valueOf(5000));
        line.setUnitOfMeasure("Cai");
        dto.setLines(Collections.singletonList(line));
        
        return salesInvoiceService.createDraft(dto, branchId).getId();
    }

    @Test
    void testIdempotencySameKeySequential() throws Exception {
        UUID invoiceId = createDraftInvoice();
        String idempotencyKey = UUID.randomUUID().toString();
        
        // Lần 1: Thành công
        mockMvc.perform(post("/api/sales-invoices/{id}/confirm", invoiceId)
                .header("Idempotency-Key", idempotencyKey)
                .header("Authorization", authToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.success").value(true));
                
        // Lần 2: Cùng key, tuần tự -> Phải trả về cache 200 OK (không lỗi, không xử lý lại)
        mockMvc.perform(post("/api/sales-invoices/{id}/confirm", invoiceId)
                .header("Idempotency-Key", idempotencyKey)
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.success").value(true));
                
        assertEquals(1, idempotencyRepo.count());
    }

    @Test
    void testIdempotencyDifferentBodySameKey() throws Exception {
        UUID invoiceId1 = createDraftInvoice();
        UUID invoiceId2 = createDraftInvoice(); // Body (URL path params) khác nhau
        String idempotencyKey = UUID.randomUUID().toString();
        
        // Lần 1: Thành công
        mockMvc.perform(post("/api/sales-invoices/{id}/confirm", invoiceId1)
                .header("Idempotency-Key", idempotencyKey)
                .header("Authorization", authToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.success").value(true));
                
        // Lần 2: Cùng key, nhưng request hash khác (do URL chứa invoiceId2 khác) -> 409 Conflict
        mockMvc.perform(post("/api/sales-invoices/{id}/confirm", invoiceId2)
                .header("Idempotency-Key", idempotencyKey)
                .header("Authorization", authToken))
                .andExpect(status().isConflict());
    }

    @Test
    void testIdempotencyBusinessRollback() throws Exception {
        UUID invoiceId = createDraftInvoice();
        String idempotencyKey = UUID.randomUUID().toString();
        
        // Ép lỗi nghiệp vụ (Invoice is not DRAFT) để test transaction rollback
        com.storename.erp.order.domain.SalesInvoice invoice = invoiceRepo.findById(invoiceId).get();
        invoice.confirm(UUID.randomUUID());
        invoiceRepo.save(invoice);
        
        // Lần 1: Thất bại do nghiệp vụ
        try {
            mockMvc.perform(post("/api/sales-invoices/{id}/confirm", invoiceId)
                    .header("Idempotency-Key", idempotencyKey)
                    .header("Authorization", authToken));
        } catch (Exception e) {}
        
        // Xác nhận record idempotency KHÔNG được lưu
        System.out.println("Idempotency records in DB: " + idempotencyRepo.count());
        for (IdempotencyRecord r : idempotencyRepo.findAll()) {
            System.out.println("Record: " + r.getIdempotencyKey() + " -> " + r.getResponseSnapshot());
        }
        assertEquals(0, idempotencyRepo.count());
    }
}
