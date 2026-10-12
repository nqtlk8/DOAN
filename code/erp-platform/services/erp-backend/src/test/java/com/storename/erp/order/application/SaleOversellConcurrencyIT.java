package com.storename.erp.order.application;

import com.storename.erp.crm.application.CustomerWriteService;
import com.storename.erp.inventory.domain.CostLayer;
import com.storename.erp.inventory.domain.StockInsufficientException;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.CostLayerRepository;
import com.storename.erp.inventory.infrastructure.StockMovementRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Không cho tồn âm khi nhiều hóa đơn được xác nhận đồng thời: tồn 10, ba hóa đơn mỗi hóa đơn 6 cái
 * -> chỉ MỘT hóa đơn được xác nhận, hai hóa đơn còn lại nhận StockInsufficientException.
 * Dòng tồn bị khóa ghi (StockOnHandRepository.findForUpdate) nên các luồng xếp hàng, không cùng
 * vượt qua bước kiểm tra. Ghi dữ liệu thật (commit) -> @DirtiesContext + DB H2 riêng.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
        "instance.role=ALL",
        "spring.datasource.url=jdbc:h2:mem:oversell_${random.uuid};DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000"
})
@ActiveProfiles("test")
public class SaleOversellConcurrencyIT {

    private static final long PRODUCT_ID = 7L;
    private static final long BRANCH_ID = 1L;

    @Autowired private SalesInvoiceService salesInvoiceService;
    @Autowired private CustomerWriteService customerWriteService;
    @Autowired private SalesInvoiceRepository invoiceRepository;
    @Autowired private StockOnHandRepository stockOnHandRepository;
    @Autowired private CostLayerRepository costLayerRepository;
    @Autowired private StockMovementRepository stockMovementRepository;

    @Test
    public void concurrentConfirms_cannotOversell() throws InterruptedException {
        com.storename.erp.crm.application.dto.CustomerCreateDto dto = new com.storename.erp.crm.application.dto.CustomerCreateDto();
        dto.setCustomerCode("CUST-OS-" + UUID.randomUUID().toString().substring(0, 5));
        dto.setName("Oversell Customer");
        dto.setPhone("0909000111");
        dto.setEmail("oversell@test.com");
        dto.setAddress("Test Address");
        dto.setTaxCode("123");
        // Khách dùng chung (branch_id NULL): CustomerWriteService yêu cầu chi nhánh tồn tại, mà test không tạo dòng branch nào.
        dto.setBranchId(null);
        UUID customerId = customerWriteService.createCustomer(dto).getId();

        StockOnHand soh = new StockOnHand(PRODUCT_ID, BRANCH_ID);
        soh.increase(new BigDecimal("10"), "INIT");
        stockOnHandRepository.save(soh);
        costLayerRepository.save(CostLayer.fromInbound(PRODUCT_ID, BRANCH_ID, new BigDecimal("10"), new BigDecimal("500"), null));

        List<UUID> invoiceIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SalesInvoice invoice = new SalesInvoice();
            invoice.setBranchId(BRANCH_ID);
            invoice.setCustomerId(customerId);
            invoice.setInvoiceCode("INV-OS-" + i + "-" + UUID.randomUUID().toString().substring(0, 5));
            SalesInvoiceLine line = new SalesInvoiceLine();
            line.setProductId(PRODUCT_ID);
            line.setProductName("SP oversell");
            line.setQuantity(new BigDecimal("6"));
            line.setUnitPrice(new BigDecimal("1000"));
            line.setLineTotal(new BigDecimal("6000"));
            line.setUnitOfMeasure("Cái");
            invoice.addLine(line);
            invoice.calculateTotal();
            invoiceIds.add(invoiceRepository.save(invoice).getId());
        }

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(3);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        for (UUID invoiceId : invoiceIds) {
            executor.submit(() -> {
                try {
                    start.await();
                    salesInvoiceService.confirmInvoice(invoiceId, UUID.randomUUID(), BRANCH_ID);
                    success.incrementAndGet();
                } catch (StockInsufficientException e) {
                    insufficient.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        Assertions.assertTrue(done.await(60, TimeUnit.SECONDS), "các luồng phải kết thúc");
        executor.shutdown();

        Assertions.assertEquals(1, success.get(), "chỉ một hóa đơn được xác nhận");
        Assertions.assertEquals(2, insufficient.get(), "hai hóa đơn còn lại bị từ chối vì không đủ hàng");

        StockOnHand stock = stockOnHandRepository.findByProductIdAndBranchId(PRODUCT_ID, BRANCH_ID).orElseThrow();
        Assertions.assertEquals(0, new BigDecimal("4").compareTo(stock.getQuantity()), "tồn còn 4, không âm");
        BigDecimal layerRemaining = costLayerRepository.findAll().stream()
                .filter(l -> l.getProductId() == PRODUCT_ID && l.getBranchId() == BRANCH_ID)
                .map(CostLayer::getRemainingQty).reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertEquals(0, new BigDecimal("4").compareTo(layerRemaining), "lớp giá vốn còn 4");
        long saleMovements = stockMovementRepository.findAll().stream()
                .filter(m -> m.getProductId() == PRODUCT_ID && "SALE".equals(m.getMovementType().name()))
                .count();
        Assertions.assertEquals(1, saleMovements);
    }
}
