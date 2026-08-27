package com.storename.erp.order.integration.branch;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.order.application.GoodsReturnService;
import com.storename.erp.order.application.dto.GoodsReturnCreateDto;
import com.storename.erp.order.domain.GoodsReturn;
import com.storename.erp.order.domain.GoodsReturnStatus;
import com.storename.erp.order.infrastructure.GoodsReturnRepository;
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
    "spring.datasource.url=jdbc:h2:mem:branch_ord_test_db_3;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=update"
})
public class GoodsReturnWithInvoiceTest {

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

    private Long branchId;
    private UUID userId;
    private UUID invoiceId;
    private Long productId;
    private Customer customer;

    @Autowired
    private com.storename.erp.order.infrastructure.SalesInvoiceRepository invoiceRepository;

    @BeforeEach
    void setUp() {
        branchId = (long)(Math.random() * 100000L);
        userId = UUID.randomUUID();
        productId = (long)(Math.random() * 100000L);

        customer = new Customer();
        customer.setCustomerCode("CUST-RET-01");
        customer.setName("Return Customer");
        customer = customerRepository.save(customer);

        com.storename.erp.order.domain.SalesInvoice invoice = new com.storename.erp.order.domain.SalesInvoice();
        invoice.setBranchId(branchId);
        invoice.setCustomerId(customer.getId());
        invoice.setInvoiceCode("INV-RET-01");
        invoice.setPaymentMethod(com.storename.erp.order.domain.PaymentMethod.CASH);
        com.storename.erp.order.domain.SalesInvoiceLine invLine = new com.storename.erp.order.domain.SalesInvoiceLine();
        invLine.setProductId(productId);
        invLine.setQuantity(new BigDecimal("5.0"));
        invLine.setUnitPrice(new BigDecimal("100000"));
        invLine.setLineTotal(invLine.getQuantity().multiply(invLine.getUnitPrice()));
        invLine.setUnitOfMeasure("Cái");
        invoice.addLine(invLine);
        invoice.confirm(userId); // set status CONFIRMED
        invoice = invoiceRepository.save(invoice);
        invoiceId = invoice.getId();

        StockOnHand stock = new StockOnHand(productId, branchId);
        stock.increase(new BigDecimal("10"), "Init"); // 10 remaining
        stock.updateAvgCost(new BigDecimal("50000"));
        stockRepository.save(stock);

        debtService.increaseDebt(customer.getId(), branchId, new BigDecimal("1000000")); // current debt 1,000,000
    }

    @Test
    void confirmReturn_WithInvoice_ShouldIncreaseStockAndDecreaseDebt() {
        GoodsReturnCreateDto dto = new GoodsReturnCreateDto();
        dto.setCustomerId(customer.getId());
        dto.setReturnCode("RET-1001");
        dto.setInvoiceId(invoiceId);
        
        GoodsReturnCreateDto.LineDto line = new GoodsReturnCreateDto.LineDto();
        line.setProductId(productId);
        line.setQuantity(new BigDecimal("2.0"));
        line.setUnitPrice(new BigDecimal("100000"));
        line.setUnitOfMeasure("Cái");
        dto.setLines(List.of(line));

        UUID returnId = returnService.createDraft(branchId, dto);

        // Act
        returnService.confirmReturn(returnId, branchId, userId);

        // Assert
        GoodsReturn gr = returnRepository.findById(returnId).orElseThrow();
        assertThat(gr.getStatus()).isEqualTo(GoodsReturnStatus.CONFIRMED);

        StockOnHand stock = stockRepository.findByProductIdAndBranchId(productId, branchId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualByComparingTo("12.0"); // 10 + 2

        BigDecimal debt = debtService.getCurrentDebt(customer.getId(), branchId);
        assertThat(debt).isEqualByComparingTo("800000"); // 1000000 - (2 * 100000)
    }
}
