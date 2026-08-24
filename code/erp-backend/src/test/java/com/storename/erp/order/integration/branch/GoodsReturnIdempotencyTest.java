package com.storename.erp.order.integration.branch;

import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import com.storename.erp.common.security.JwtTokenProvider;
import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.order.application.GoodsReturnService;
import com.storename.erp.order.application.dto.GoodsReturnCreateDto;
import com.storename.erp.order.infrastructure.GoodsReturnRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"branch", "test"})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_ord_test_db_6;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=update"
})
public class GoodsReturnIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private GoodsReturnService returnService;

    @Autowired
    private GoodsReturnRepository returnRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ReceivableDebtService debtService;

    @Autowired
    private StockOnHandRepository stockRepository;

    @Autowired
    private IdempotencyRecordRepository idempotencyRepo;

    private UUID branchId;
    private UUID invoiceId;
    private UUID productId;
    private Customer customer;
    private String authToken;

    @Autowired
    private com.storename.erp.order.infrastructure.SalesInvoiceRepository invoiceRepository;
    
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM receivable_debt");
        jdbcTemplate.execute("DELETE FROM goods_return_line");
        jdbcTemplate.execute("DELETE FROM goods_return");
        jdbcTemplate.execute("DELETE FROM stock_on_hand");
        jdbcTemplate.execute("DELETE FROM sales_invoice_line");
        jdbcTemplate.execute("DELETE FROM sales_invoice");
        jdbcTemplate.execute("DELETE FROM customer");
        jdbcTemplate.execute("DELETE FROM idempotency_record");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        branchId = UUID.randomUUID();
        productId = UUID.randomUUID();

        authToken = "Bearer " + jwtTokenProvider.generateToken("testuser", "ADMIN", branchId.toString(), UUID.randomUUID().toString());

        customer = new Customer();
        customer.setCustomerCode("CUST-IDEM-01");
        customer.setName("Idem Customer");
        customer = customerRepository.save(customer);

        com.storename.erp.order.domain.SalesInvoice invoice = new com.storename.erp.order.domain.SalesInvoice();
        invoice.setBranchId(branchId);
        invoice.setCustomerId(customer.getId());
        invoice.setInvoiceCode("INV-RET-IDEM-01");
        invoice.setPaymentMethod(com.storename.erp.order.domain.PaymentMethod.CASH);
        com.storename.erp.order.domain.SalesInvoiceLine invLine = new com.storename.erp.order.domain.SalesInvoiceLine();
        invLine.setProductId(productId);
        invLine.setQuantity(new BigDecimal("5.0"));
        invLine.setUnitPrice(new BigDecimal("100000"));
        invLine.setLineTotal(invLine.getQuantity().multiply(invLine.getUnitPrice()));
        invLine.setUnitOfMeasure("Cái");
        invoice.addLine(invLine);
        invoice.confirm(UUID.randomUUID()); 
        invoice = invoiceRepository.save(invoice);
        invoiceId = invoice.getId();

        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("10"), "Init"); // 10 remaining
        stock.updateAvgCost(new BigDecimal("50000"));
        stockRepository.save(stock);

        debtService.increaseDebt(customer.getId(), branchId, new BigDecimal("1000000")); // current debt 1,000,000
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM receivable_debt");
        jdbcTemplate.execute("DELETE FROM goods_return_line");
        jdbcTemplate.execute("DELETE FROM goods_return");
        jdbcTemplate.execute("DELETE FROM stock_on_hand");
        jdbcTemplate.execute("DELETE FROM customer");
        jdbcTemplate.execute("DELETE FROM idempotency_record");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    void confirmReturn_ConcurrentSameKey_ShouldExecuteOnlyOnce() {
        GoodsReturnCreateDto dto = new GoodsReturnCreateDto();
        dto.setCustomerId(customer.getId());
        dto.setReturnCode("RET-IDEM-01");
        dto.setInvoiceId(invoiceId);
        
        GoodsReturnCreateDto.LineDto line = new GoodsReturnCreateDto.LineDto();
        line.setProductId(productId);
        line.setQuantity(new BigDecimal("2.0"));
        line.setUnitPrice(new BigDecimal("100000"));
        line.setUnitOfMeasure("Cái");
        dto.setLines(List.of(line));

        UUID returnId = returnService.createDraft(branchId, dto);

        String idempotencyKey = "RET-KEY-" + UUID.randomUUID();

        CompletableFuture<Void> req1 = CompletableFuture.runAsync(() -> {
            try {
                mockMvc.perform(post("/api/v1/goods-returns/" + returnId + "/confirm")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
            } catch (Exception e) {
                // Do nothing, exception is expected in one of the threads due to unique constraint or stale state
            }
        });
        
        CompletableFuture<Void> req2 = CompletableFuture.runAsync(() -> {
            try {
                mockMvc.perform(post("/api/v1/goods-returns/" + returnId + "/confirm")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk());
            } catch (Exception e) {
            }
        });

        CompletableFuture.allOf(req1, req2).join();

        // 1 record in idempotency_record
        assertThat(idempotencyRepo.count()).isEqualTo(1);

        // Stock increases only once
        StockOnHand stock = stockRepository.findByProductIdAndBranchId(productId, branchId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualByComparingTo("12.0");

        // Debt decreases only once
        BigDecimal debt = debtService.getCurrentDebt(customer.getId(), branchId);
        assertThat(debt).isEqualByComparingTo("800000");
    }
}
