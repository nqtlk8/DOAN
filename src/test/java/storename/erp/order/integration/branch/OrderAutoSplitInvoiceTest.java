package com.storename.erp.order.integration.branch;

import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.order.application.CustomerOrderService;
import com.storename.erp.order.application.dto.CustomerOrderCreateDto;
import com.storename.erp.order.domain.CustomerOrder;
import com.storename.erp.order.domain.CustomerOrderStatus;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.infrastructure.CustomerOrderRepository;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles({"branch", "test"})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_ord_test_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=update"
})
public class OrderAutoSplitInvoiceTest {

    @Autowired
    private CustomerOrderService orderService;

    @Autowired
    private CustomerOrderRepository orderRepository;

    @Autowired
    private SalesInvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private UUID branchId;
    private UUID userId;
    private Customer customer;

    @BeforeEach
    void setUp() {
        branchId = UUID.randomUUID();
        userId = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerCode("CUST-AUTO-01");
        customer.setName("Auto Test Customer");
        customer = customerRepository.save(customer);
    }

    @Test
    void confirmOrder_ShouldAutoCreateAndConfirmSalesInvoice() {
        CustomerOrderCreateDto dto = new CustomerOrderCreateDto();
        dto.setCustomerId(customer.getId());
        dto.setOrderCode("ORD-1001");
        
        CustomerOrderCreateDto.LineDto line = new CustomerOrderCreateDto.LineDto();
        line.setProductId(UUID.randomUUID());
        line.setQuantity(new BigDecimal("5.0"));
        line.setUnitPrice(new BigDecimal("100000"));
        line.setUnitOfMeasure("Cái");
        dto.setLines(List.of(line));

        UUID orderId = orderService.createDraft(branchId, dto);

        // Act
        orderService.confirmOrder(orderId, branchId, userId);

        // Assert
        CustomerOrder order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(CustomerOrderStatus.INVOICED);

        List<SalesInvoice> invoices = invoiceRepository.findAll();
        assertThat(invoices).hasSize(1);
        
        SalesInvoice invoice = invoices.get(0);
        assertThat(invoice.getInvoiceCode()).isEqualTo("INV-AUTO-ORD-1001");
        assertThat(invoice.getStatus().name()).isEqualTo("CONFIRMED");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("500000");
    }
}
