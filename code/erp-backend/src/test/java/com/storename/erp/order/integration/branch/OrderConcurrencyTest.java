package com.storename.erp.order.integration.branch;

import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.order.application.CustomerOrderService;
import com.storename.erp.order.application.dto.CustomerOrderCreateDto;
import com.storename.erp.order.infrastructure.CustomerOrderRepository;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"branch", "test"})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_ord_test_db_2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=update"
})
public class OrderConcurrencyTest {

    @Autowired
    private CustomerOrderService orderService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StockOnHandRepository stockRepository;
    
    @Autowired
    private CustomerOrderRepository orderRepository;
    
    @Autowired
    private SalesInvoiceRepository invoiceRepository;

    private UUID branchId;
    private UUID userId;
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
        jdbcTemplate.execute("DELETE FROM stock_on_hand");
        jdbcTemplate.execute("DELETE FROM customer");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        branchId = UUID.randomUUID();
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerCode("CUST-CONC-01");
        customer.setName("Conc Test Customer");
        customer = customerRepository.save(customer);

        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("100"), "Init");
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
        jdbcTemplate.execute("DELETE FROM stock_on_hand");
        jdbcTemplate.execute("DELETE FROM customer");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    void concurrentOrderConfirms_ShouldMaintainStockInvariant() {
        int threadCount = 10;
        List<UUID> orderIds = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            CustomerOrderCreateDto dto = new CustomerOrderCreateDto();
            dto.setCustomerId(customer.getId());
            dto.setOrderCode("ORD-CONC-" + i);
            
            CustomerOrderCreateDto.LineDto line = new CustomerOrderCreateDto.LineDto();
            line.setProductId(productId);
            line.setQuantity(new BigDecimal("2.0"));
            line.setUnitPrice(new BigDecimal("100000"));
            line.setUnitOfMeasure("Cái");
            dto.setLines(List.of(line));

            orderIds.add(orderService.createDraft(branchId, dto));
        }

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        List<CompletableFuture<Void>> futures = orderIds.stream().map(orderId -> CompletableFuture.runAsync(() -> {
            try {
                orderService.confirmOrder(orderId, branchId, userId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            }
        })).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        StockOnHand stock = stockRepository.findByProductIdAndBranchId(productId, branchId).orElseThrow();
        
        // Initial stock = 100. Each order takes 2.
        BigDecimal expectedRemaining = new BigDecimal("100").subtract(new BigDecimal("2.0").multiply(new BigDecimal(successCount.get())));
        
        assertThat(stock.getQuantity()).isEqualByComparingTo(expectedRemaining);
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
    }
}
