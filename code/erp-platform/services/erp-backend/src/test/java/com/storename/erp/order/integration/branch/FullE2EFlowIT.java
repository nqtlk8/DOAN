package com.storename.erp.order.integration.branch;

import com.storename.erp.branch.domain.Branch;
import com.storename.erp.branch.infrastructure.BranchRepository;
import com.storename.erp.catalog.domain.Category;
import com.storename.erp.catalog.domain.Product;
import com.storename.erp.catalog.infrastructure.CategoryRepository;
import com.storename.erp.catalog.infrastructure.ProductRepository;
import com.storename.erp.crm.application.CustomerWriteService;
import com.storename.erp.crm.application.dto.CustomerCreateDto;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.domain.ReceivableDebt;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.crm.infrastructure.ReceivableDebtRepository;
import com.storename.erp.inventory.application.InboundReceiptService;
import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.order.application.GoodsReturnService;
import com.storename.erp.order.application.SalesInvoiceService;
import com.storename.erp.order.application.dto.GoodsReturnCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceLineDto;
import com.storename.erp.order.domain.SalesInvoice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = { "instance.role=ALL" })
@ActiveProfiles("test")
public class FullE2EFlowIT {

    @Autowired private BranchRepository branchRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CustomerWriteService customerWriteService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private InboundReceiptService inboundReceiptService;
    @Autowired private SalesInvoiceService salesInvoiceService;
    @Autowired private GoodsReturnService goodsReturnService;
    @Autowired private StockOnHandRepository stockOnHandRepository;
    @Autowired private ReceivableDebtRepository receivableDebtRepository;
    @Autowired private com.storename.erp.crm.application.ReceivableDebtService debtService;

    @Test
    public void testFullE2EFlow() {
        // 1. Setup Master Data
        Branch branch = new Branch();
        branch.setName("Main E2E Branch");
        branch.setAddress("Hanoi");
        branch.setCode("BR-E2E");
        branchRepository.save(branch);
        Long branchId = branch.getId();

        Category category = Category.builder()
            .name("Electronics")
            .build();
        categoryRepository.save(category);

        Product product = Product.builder()
            .category(category)
            .code("PROD-E2E")
            .name("E2E Laptop")
            .baseUnit("Cai")
            .build();
        productRepository.save(product);
        Long productId = product.getId();

        CustomerCreateDto custDto = new CustomerCreateDto();
        custDto.setName("E2E Customer");
        custDto.setPhone("0987654321");
        custDto.setCustomerCode("CUST-E2E");
        UUID customerId = customerWriteService.createCustomer(custDto).getId();

        // 2. Inbound Goods (Nhập kho) -> Stock goes up
        InboundReceiptCreateDto receiptDto = new InboundReceiptCreateDto();
        receiptDto.setReceiptCode("RC-E2E-1");
        receiptDto.setSupplierId(UUID.randomUUID()); // Dummy supplier
        InboundReceiptCreateDto.LineDto receiptLine = new InboundReceiptCreateDto.LineDto();
        receiptLine.setProductId(productId);
        receiptLine.setQuantity(new BigDecimal("100"));
        receiptLine.setUnitCost(new BigDecimal("1000"));
        receiptLine.setUnitOfMeasure("Cai");
        receiptDto.setLines(List.of(receiptLine));
        
        UUID receiptId = inboundReceiptService.createDraft(branchId, receiptDto);
        inboundReceiptService.confirmReceipt(receiptId, branchId, UUID.randomUUID());

        StockOnHand stock = stockOnHandRepository.findByProductIdAndBranchId(productId, branchId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualByComparingTo("100");

        // 3. Sales Invoice (Bán hàng) -> Stock goes down, Debt goes up
        SalesInvoiceCreateDto invoiceDto = new SalesInvoiceCreateDto();
        invoiceDto.setCustomerId(customerId);
        invoiceDto.setInvoiceCode("INV-E2E-1");
        SalesInvoiceLineDto invoiceLine = new SalesInvoiceLineDto();
        invoiceLine.setProductId(productId);
        invoiceLine.setQuantity(new BigDecimal("10"));
        invoiceLine.setUnitPrice(new BigDecimal("1500"));
        invoiceLine.setUnitOfMeasure("Cai");
        invoiceDto.setLines(List.of(invoiceLine));

        SalesInvoice invoice = salesInvoiceService.createDraft(invoiceDto, branchId);
        salesInvoiceService.confirmInvoice(invoice.getId(), UUID.randomUUID(), branchId);

        // Check Stock: 100 - 10 = 90
        stock = stockOnHandRepository.findByProductIdAndBranchId(productId, branchId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualByComparingTo("90");

        // Check Debt: 10 * 1500 = 15000
        ReceivableDebt debt = receivableDebtRepository.findByCustomerIdAndBranchId(customerId, branchId).orElseThrow();
        assertThat(debt.getTotalDebt()).isEqualByComparingTo("15000");

        // 4. Customer Payment (Thu tiền nợ) -> Debt goes down
        debtService.decreaseDebt(customerId, branchId, new BigDecimal("5000"), com.storename.erp.crm.domain.ReceivableDebtMovementType.PAYMENT, "PAYMENT", "PAY-E2E-1", UUID.randomUUID(), "Payment");
        debt = receivableDebtRepository.findByCustomerIdAndBranchId(customerId, branchId).orElseThrow();
        assertThat(debt.getTotalDebt()).isEqualByComparingTo("10000");

        // 5. Goods Return (Khách trả hàng) -> Stock goes up, Debt goes down
        GoodsReturnCreateDto returnDto = new GoodsReturnCreateDto();
        returnDto.setCustomerId(customerId);
        returnDto.setInvoiceId(invoice.getId());
        returnDto.setReturnCode("RET-E2E-1");
        GoodsReturnCreateDto.LineDto returnLine = new GoodsReturnCreateDto.LineDto();
        returnLine.setProductId(productId);
        returnLine.setQuantity(new BigDecimal("2"));
        returnLine.setUnitPrice(new BigDecimal("1500"));
        returnLine.setUnitOfMeasure("Cai");
        returnDto.setLines(List.of(returnLine));

        UUID returnId = goodsReturnService.createDraft(branchId, returnDto);
        goodsReturnService.confirmReturn(returnId, branchId, UUID.randomUUID());

        // Check Stock: 90 + 2 = 92
        stock = stockOnHandRepository.findByProductIdAndBranchId(productId, branchId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualByComparingTo("92");

        // Check Debt: 10000 - (2 * 1500) = 7000
        debt = receivableDebtRepository.findByCustomerIdAndBranchId(customerId, branchId).orElseThrow();
        assertThat(debt.getTotalDebt()).isEqualByComparingTo("7000");
    }
}
