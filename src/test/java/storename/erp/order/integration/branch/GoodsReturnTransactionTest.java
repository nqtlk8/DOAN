package com.storename.erp.order.integration.branch;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles({"branch", "test"})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_ord_test_db_5;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=update"
})
public class GoodsReturnTransactionTest {

    @Autowired
    private GoodsReturnService returnService;

    @Autowired
    private GoodsReturnRepository returnRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @SpyBean
    private ReceivableDebtService debtService;

    @Autowired
    private StockOnHandRepository stockRepository;

    private UUID branchId;
    private UUID userId;
    private UUID invoiceId;
    private UUID productId;
    private Customer customer;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM receivable_debt");
        jdbcTemplate.execute("DELETE FROM sales_invoice_line");
        jdbcTemplate.execute("DELETE FROM sales_invoice");
        jdbcTemplate.execute("DELETE FROM customer_order_line");
        jdbcTemplate.execute("DELETE FROM customer_order");
        jdbcTemplate.execute("DELETE FROM goods_return_line");
        jdbcTemplate.execute("DELETE FROM goods_return");
        jdbcTemplate.execute("DELETE FROM stock_on_hand");
        jdbcTemplate.execute("DELETE FROM idempotency_record");
        jdbcTemplate.execute("DELETE FROM customer");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        branchId = UUID.randomUUID();
        userId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();
        productId = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerCode("CUST-TX-01");
        customer.setName("Tx Customer");
        customer = customerRepository.save(customer);

        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("10"), "Init"); // 10 remaining
        stock.updateAvgCost(new BigDecimal("50000"));
        stockRepository.save(stock);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM receivable_debt");
        jdbcTemplate.execute("DELETE FROM sales_invoice_line");
        jdbcTemplate.execute("DELETE FROM sales_invoice");
        jdbcTemplate.execute("DELETE FROM customer_order_line");
        jdbcTemplate.execute("DELETE FROM customer_order");
        jdbcTemplate.execute("DELETE FROM goods_return_line");
        jdbcTemplate.execute("DELETE FROM goods_return");
        jdbcTemplate.execute("DELETE FROM stock_on_hand");
        jdbcTemplate.execute("DELETE FROM idempotency_record");
        jdbcTemplate.execute("DELETE FROM customer");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    void confirmReturn_WhenErrorOccurs_ShouldRollbackEverything() {
        GoodsReturnCreateDto dto = new GoodsReturnCreateDto();
        dto.setCustomerId(customer.getId());
        dto.setReturnCode("RET-1003");
        dto.setInvoiceId(invoiceId);
        
        GoodsReturnCreateDto.LineDto line = new GoodsReturnCreateDto.LineDto();
        line.setProductId(productId);
        line.setQuantity(new BigDecimal("2.0"));
        line.setUnitPrice(new BigDecimal("100000"));
        line.setUnitOfMeasure("Cái");
        dto.setLines(List.of(line));

        UUID returnId = returnService.createDraft(branchId, dto);

        // Giả lập lỗi khi gọi debtService
        doThrow(new RuntimeException("Simulated error in debt service")).when(debtService).decreaseDebt(any(), any(), any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            returnService.confirmReturn(returnId, branchId, userId);
        });

        // Stock must not increase
        StockOnHand stock = stockRepository.findByProductIdAndBranchId(productId, branchId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualByComparingTo("10.0"); // still 10

        // Goods return status must still be DRAFT
        var gr = returnRepository.findById(returnId).orElseThrow();
        assertThat(gr.getStatus().name()).isEqualTo("DRAFT");
    }
}
