package com.storename.erp.postgres;


import com.storename.erp.support.PostgresIntegrationTest;
import com.storename.erp.common.domain.IdempotencyRecord;
import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.inventory.application.InboundReceiptService;
import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.domain.StockInsufficientException;
import com.storename.erp.order.application.GoodsReturnService;
import com.storename.erp.order.application.SalesInvoiceService;
import com.storename.erp.order.application.dto.GoodsReturnCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceLineDto;
import com.storename.erp.order.domain.PaymentMethod;
import com.storename.erp.order.domain.SalesInvoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra đồng thời trên PostgreSQL thật (Testcontainers) cho các sửa đổi nghiệp vụ lõi:
 * thứ tự khóa dòng tồn (không deadlock), tạo dòng tồn đầu tiên đồng thời, khóa dòng công nợ,
 * số lượng 4 chữ số thập phân (migration V13), cộng dồn số lượng trả hàng, hoàn tiền khi khách đã trả đủ.
 *
 * <p>Không dùng @Transactional: các luồng phải commit thật để tranh khóa với nhau. Mỗi test dùng
 * sản phẩm riêng (dải 97001+) và khách riêng nên không ảnh hưởng nhau hay các lớp IT khác cùng container.
 *
 * <p>Chạy: {@code mvn verify -Dit.test=CoreBusinessConcurrencyPostgresIT} (cần Docker).
 */
@SpringBootTest
@ActiveProfiles("postgres-it")
class CoreBusinessConcurrencyPostgresIT extends PostgresIntegrationTest {

    private static final long BRANCH = 9701L;
    private static final long CATEGORY = 9700L;
    private static final UUID USER = UUID.randomUUID();

    @Autowired private SalesInvoiceService salesInvoiceService;
    @Autowired private InboundReceiptService inboundReceiptService;
    @Autowired private GoodsReturnService goodsReturnService;
    @Autowired private ReceivableDebtService debtService;
    @Autowired private IdempotencyRecordRepository idempotencyRepo;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void fixtures() {
        jdbc.update("""
                INSERT INTO branch (id, code, name, is_active, created_at, updated_at)
                VALUES (?, 'IT-CONC', 'Chi nhánh IT đồng thời', true, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, BRANCH);
        jdbc.update("""
                INSERT INTO category (id, code, name, is_active, created_at, updated_at)
                VALUES (?, 'IT-CONC-CAT', 'Danh mục IT đồng thời', true, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, CATEGORY);
        for (long id = 97001; id <= 97012; id++) {
            jdbc.update("""
                    INSERT INTO product (id, code, name, category_id, base_unit, is_active, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'Cái', true, now(), now())
                    ON CONFLICT (id) DO NOTHING
                    """, id, "IT-CC-" + id, "SP đồng thời " + id, CATEGORY);
        }
    }

    // ------------------------------------------------------------------ helpers

    private UUID newCustomer() {
        UUID id = UUID.randomUUID();
        String code = "IT-CC-" + id.toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO customer (id, customer_code, name, is_deleted, version, created_at, updated_at)
                VALUES (?, ?, ?, false, 0, now(), now())
                """, id, code, "Khách " + code);
        return id;
    }

    private UUID draftReceipt(long[] productIds, String qty, String unitCost) {
        InboundReceiptCreateDto dto = new InboundReceiptCreateDto();
        List<InboundReceiptCreateDto.LineDto> lines = new ArrayList<>();
        for (long pid : productIds) {
            InboundReceiptCreateDto.LineDto l = new InboundReceiptCreateDto.LineDto();
            l.setProductId(pid);
            l.setQuantity(new BigDecimal(qty));
            l.setUnitCost(new BigDecimal(unitCost));
            l.setUnitOfMeasure("Cái");
            lines.add(l);
        }
        dto.setLines(lines);
        return inboundReceiptService.createDraft(BRANCH, dto);
    }

    private void receive(long productId, String qty, String unitCost) {
        UUID id = draftReceipt(new long[]{productId}, qty, unitCost);
        inboundReceiptService.confirmReceipt(id, BRANCH, USER);
    }

    private SalesInvoiceCreateDto saleDto(UUID customerId, String qty, String unitPrice, long... productIds) {
        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setCustomerId(customerId);
        dto.setPaymentMethod(PaymentMethod.CREDIT);
        List<SalesInvoiceLineDto> lines = new ArrayList<>();
        for (long pid : productIds) {
            SalesInvoiceLineDto l = new SalesInvoiceLineDto();
            l.setProductId(pid);
            l.setProductName("SP " + pid);
            l.setQuantity(new BigDecimal(qty));
            l.setUnitPrice(new BigDecimal(unitPrice));
            l.setUnitOfMeasure("Cái");
            lines.add(l);
        }
        dto.setLines(lines);
        return dto;
    }

    private UUID draftReturnLines(UUID customerId, UUID invoiceId, String unitPrice, long productId, String... quantities) {
        GoodsReturnCreateDto dto = new GoodsReturnCreateDto();
        dto.setCustomerId(customerId);
        dto.setInvoiceId(invoiceId);
        List<GoodsReturnCreateDto.LineDto> lines = new ArrayList<>();
        for (String q : quantities) {
            GoodsReturnCreateDto.LineDto l = new GoodsReturnCreateDto.LineDto();
            l.setProductId(productId);
            l.setQuantity(new BigDecimal(q));
            l.setUnitPrice(new BigDecimal(unitPrice));
            l.setUnitOfMeasure("Cái");
            lines.add(l);
        }
        dto.setLines(lines);
        return goodsReturnService.createDraft(BRANCH, dto);
    }

    /** Chạy các tác vụ cùng lúc; phần tử kết quả là null nếu thành công, ngược lại là ngoại lệ. */
    private List<Throwable> runConcurrently(List<Callable<?>> tasks) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(tasks.size());
        Throwable[] results = new Throwable[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    tasks.get(idx).call();
                } catch (Throwable t) {
                    results[idx] = t;
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(120, TimeUnit.SECONDS)).as("mọi luồng phải kết thúc").isTrue();
        pool.shutdownNow();
        return java.util.Arrays.asList(results);
    }

    private BigDecimal stock(long productId) {
        BigDecimal q = jdbc.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM stock_on_hand WHERE product_id = ? AND branch_id = ?",
                BigDecimal.class, productId, BRANCH);
        return q == null ? BigDecimal.ZERO : q;
    }

    private BigDecimal layerRemaining(long productId) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(remaining_qty), 0) FROM cost_layer WHERE product_id = ? AND branch_id = ?",
                BigDecimal.class, productId, BRANCH);
    }

    private BigDecimal movementSum(long productId) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM stock_movement WHERE product_id = ? AND branch_id = ?",
                BigDecimal.class, productId, BRANCH);
    }

    /** Bất biến kho: stock_on_hand = SUM(stock_movement) = SUM(cost_layer.remaining_qty). */
    private void assertInventoryConsistent(long productId, String expected) {
        assertThat(stock(productId)).as("stock_on_hand của " + productId).isEqualByComparingTo(expected);
        assertThat(movementSum(productId)).as("tổng stock_movement của " + productId).isEqualByComparingTo(expected);
        assertThat(layerRemaining(productId)).as("tổng remaining_qty của " + productId).isEqualByComparingTo(expected);
    }

    private long deadlockCount() throws InterruptedException {
        // Thống kê của PostgreSQL được ghi dồn (tối đa ~1 giây); chờ rồi bỏ snapshot cũ của phiên.
        Thread.sleep(1500);
        jdbc.execute("SELECT pg_stat_clear_snapshot()");
        Long n = jdbc.queryForObject(
                "SELECT deadlocks FROM pg_stat_database WHERE datname = current_database()", Long.class);
        return n == null ? 0 : n;
    }

    private static void assertAllSucceeded(List<Throwable> results) {
        List<Throwable> failures = results.stream().filter(java.util.Objects::nonNull).toList();
        failures.forEach(Throwable::printStackTrace);
        assertThat(failures).as("không luồng nào được lỗi").isEmpty();
    }

    // ------------------------------------------------------------------ tests

    @Test
    @DisplayName("20 hóa đơn đồng thời, nửa có dòng [A,B], nửa [B,A]: tất cả thành công, không deadlock")
    void crossOrderSales_noDeadlock() throws Exception {
        long a = 97001, b = 97002;
        receive(a, "100", "1000");
        receive(b, "100", "2000");
        List<UUID> customers = new ArrayList<>();
        for (int i = 0; i < 20; i++) customers.add(newCustomer());
        long deadlocksBefore = deadlockCount();

        List<Callable<?>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            UUID c = customers.get(i);
            long[] order = (i % 2 == 0) ? new long[]{a, b} : new long[]{b, a};
            tasks.add(() -> salesInvoiceService.createAndConfirm(saleDto(c, "1", "5000", order), BRANCH, USER));
        }
        List<Throwable> results = runConcurrently(tasks);

        assertAllSucceeded(results);
        assertInventoryConsistent(a, "80");
        assertInventoryConsistent(b, "80");
        assertThat(deadlockCount()).as("PostgreSQL không phát hiện deadlock nào").isEqualTo(deadlocksBefore);
    }

    @Test
    @DisplayName("Bán [C,D], nhập [D,C], trả hàng [D,C] chạy xen kẽ: tất cả thành công, kho nhất quán")
    void mixedFlows_reverseOrder_noDeadlock() throws Exception {
        long c = 97003, d = 97004;
        receive(c, "50", "1000");
        receive(d, "50", "1000");
        UUID returnCustomer = newCustomer();
        List<UUID> receipts = new ArrayList<>();
        List<UUID> returns = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            receipts.add(draftReceipt(new long[]{d, c}, "2", "1100"));
            // Phiếu trả không có hóa đơn gốc: nhập lại kho như nhập hàng (hai dòng D rồi C).
            GoodsReturnCreateDto dto = new GoodsReturnCreateDto();
            dto.setCustomerId(returnCustomer);
            List<GoodsReturnCreateDto.LineDto> lines = new ArrayList<>();
            for (long pid : new long[]{d, c}) {
                GoodsReturnCreateDto.LineDto l = new GoodsReturnCreateDto.LineDto();
                l.setProductId(pid);
                l.setQuantity(BigDecimal.ONE);
                l.setUnitPrice(BigDecimal.ZERO);
                l.setUnitOfMeasure("Cái");
                lines.add(l);
            }
            dto.setLines(lines);
            returns.add(goodsReturnService.createDraft(BRANCH, dto));
        }
        List<UUID> saleCustomers = new ArrayList<>();
        for (int i = 0; i < 6; i++) saleCustomers.add(newCustomer());
        long deadlocksBefore = deadlockCount();

        List<Callable<?>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            UUID sc = saleCustomers.get(i);
            UUID r = receipts.get(i);
            UUID gr = returns.get(i);
            tasks.add(() -> salesInvoiceService.createAndConfirm(saleDto(sc, "1", "5000", c, d), BRANCH, USER));
            tasks.add(() -> { inboundReceiptService.confirmReceipt(r, BRANCH, USER); return null; });
            tasks.add(() -> { goodsReturnService.confirmReturn(gr, BRANCH, USER); return null; });
        }
        List<Throwable> results = runConcurrently(tasks);

        assertAllSucceeded(results);
        // 50 - 6 (bán) + 12 (nhập) + 6 (trả) = 62
        assertInventoryConsistent(c, "62");
        assertInventoryConsistent(d, "62");
        assertThat(deadlockCount()).isEqualTo(deadlocksBefore);
    }

    @Test
    @DisplayName("8 phiếu nhập đồng thời cho sản phẩm chưa có dòng tồn: tất cả thành công, chỉ một dòng stock_on_hand")
    void concurrentReceipts_createFirstStockRowOnce() throws Exception {
        long p = 97005;
        List<UUID> receipts = new ArrayList<>();
        for (int i = 0; i < 8; i++) receipts.add(draftReceipt(new long[]{p}, "5", "1000"));

        List<Callable<?>> tasks = new ArrayList<>();
        for (UUID r : receipts) tasks.add(() -> { inboundReceiptService.confirmReceipt(r, BRANCH, USER); return null; });
        List<Throwable> results = runConcurrently(tasks);

        assertAllSucceeded(results);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_on_hand WHERE product_id = ? AND branch_id = ?",
                Integer.class, p, BRANCH)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cost_layer WHERE product_id = ? AND branch_id = ?",
                Integer.class, p, BRANCH)).isEqualTo(8);
        assertInventoryConsistent(p, "40");
    }

    @Test
    @DisplayName("10 lần thu nợ 150.000 đồng thời trên nợ 1.000.000: đúng 6 thành công, 4 bị từ chối, nợ còn 100.000")
    void concurrentPayments_neverOverdraw() throws Exception {
        UUID customer = newCustomer();
        debtService.setOpeningBalance(customer, BRANCH, new BigDecimal("1000000"), USER, "Đầu kỳ");

        List<Callable<?>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> debtService.recordPayment(customer, BRANCH, new BigDecimal("150000"), USER, "Thu nợ"));
        }
        List<Throwable> results = runConcurrently(tasks);

        long ok = results.stream().filter(java.util.Objects::isNull).count();
        List<Throwable> rejected = results.stream().filter(java.util.Objects::nonNull).toList();
        rejected.stream().filter(t -> !(t instanceof BusinessConflictException)).forEach(Throwable::printStackTrace);
        assertThat(ok).isEqualTo(6);
        assertThat(rejected).hasSize(4).allSatisfy(t -> assertThat(t)
                .isInstanceOf(BusinessConflictException.class)
                .extracting("code").isEqualTo(ReceivableDebtService.DEBT_NEGATIVE));

        assertThat(debtService.getCurrentDebt(customer, BRANCH)).isEqualByComparingTo("100000");
        BigDecimal ledger = jdbc.queryForObject(
                "SELECT SUM(amount) FROM receivable_debt_movement WHERE customer_id = ? AND branch_id = ?",
                BigDecimal.class, customer, BRANCH);
        assertThat(ledger).as("sổ biến động khớp số dư").isEqualByComparingTo("100000");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM receivable_debt_movement
                WHERE customer_id = ? AND branch_id = ? AND ref_type = 'DEBT_PAYMENT'
                """, Integer.class, customer, BRANCH)).isEqualTo(6);
    }

    @Test
    @DisplayName("10 hóa đơn đồng thời cùng một khách: đều thành công, nợ cũ/nợ còn lại nối tiếp nhau không trùng")
    void concurrentSalesSameCustomer_debtSnapshotsChain() throws Exception {
        long p = 97006;
        receive(p, "100", "1000");
        UUID customer = newCustomer();

        List<Callable<?>> tasks = new ArrayList<>();
        List<SalesInvoice> invoices = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> invoices.add(salesInvoiceService.createAndConfirm(saleDto(customer, "1", "10000", p), BRANCH, USER)));
        }
        assertAllSucceeded(runConcurrently(tasks));

        assertThat(debtService.getCurrentDebt(customer, BRANCH)).isEqualByComparingTo("100000");
        List<SalesInvoice> chain = invoices.stream().sorted(Comparator.comparing(SalesInvoice::getPreviousDebt)).toList();
        for (int i = 0; i < chain.size(); i++) {
            assertThat(chain.get(i).getPreviousDebt()).isEqualByComparingTo(new BigDecimal(10000L * i));
            assertThat(chain.get(i).getRemainingDebt()).isEqualByComparingTo(new BigDecimal(10000L * (i + 1)));
        }
        assertInventoryConsistent(p, "90");
    }

    @Test
    @DisplayName("Số lượng 4 chữ số thập phân: nhập 10,0004 rồi bán đúng 10,0004 được, bán thêm 0,0001 bị từ chối")
    void fractionalQuantity_roundTrip() {
        long p = 97007;
        receive(p, "10.0004", "1000");
        UUID customer = newCustomer();

        salesInvoiceService.createAndConfirm(saleDto(customer, "10.0004", "2000", p), BRANCH, USER);
        assertInventoryConsistent(p, "0");

        assertThatThrownBy(() -> salesInvoiceService.createAndConfirm(saleDto(customer, "0.0001", "2000", p), BRANCH, USER))
                .isInstanceOf(StockInsufficientException.class);
    }

    @Test
    @DisplayName("Hai phiếu trả cùng hóa đơn (6 và 3+3) xác nhận đồng thời trên 10 đã bán: chỉ một phiếu được nhận")
    void concurrentReturns_withDuplicateLines_respectSoldQuantity() throws Exception {
        long p = 97008;
        receive(p, "20", "1000");
        UUID customer = newCustomer();
        SalesInvoice invoice = salesInvoiceService.createAndConfirm(saleDto(customer, "10", "5000", p), BRANCH, USER);

        UUID r1 = draftReturnLines(customer, invoice.getId(), "5000", p, "6");
        UUID r2 = draftReturnLines(customer, invoice.getId(), "5000", p, "3", "3");
        List<Throwable> results = runConcurrently(List.of(
                () -> { goodsReturnService.confirmReturn(r1, BRANCH, USER); return null; },
                () -> { goodsReturnService.confirmReturn(r2, BRANCH, USER); return null; }));

        assertThat(results.stream().filter(java.util.Objects::isNull).count()).isEqualTo(1);
        assertThat(results.stream().filter(java.util.Objects::nonNull).toList())
                .singleElement().isInstanceOf(IllegalArgumentException.class);
        BigDecimal returned = jdbc.queryForObject("""
                SELECT COALESCE(SUM(l.quantity), 0) FROM goods_return_line l JOIN goods_return r ON r.id = l.return_id
                WHERE r.invoice_id = ? AND r.status = 'CONFIRMED'
                """, BigDecimal.class, invoice.getId());
        assertThat(returned).isEqualByComparingTo("6");
        assertInventoryConsistent(p, "16");

        // Một phiếu riêng lẻ có hai dòng 6 + 6 > 10 cũng bị từ chối.
        UUID r3 = draftReturnLines(customer, invoice.getId(), "5000", p, "6", "6");
        assertThatThrownBy(() -> goodsReturnService.confirmReturn(r3, BRANCH, USER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Khách đã trả trước đủ rồi trả hàng: phiếu vẫn xác nhận, nợ giữ 0, refund_amount = tiền hàng trả")
    void returnAfterFullPrepayment_recordsRefund() {
        long p = 97009;
        receive(p, "10", "1000");
        UUID customer = newCustomer();
        SalesInvoiceCreateDto dto = saleDto(customer, "2", "100000", p);
        dto.setPaymentMethod(PaymentMethod.CASH);
        dto.setAdvancePayment(new BigDecimal("200000"));
        SalesInvoice invoice = salesInvoiceService.createAndConfirm(dto, BRANCH, USER);
        assertThat(debtService.getCurrentDebt(customer, BRANCH)).isEqualByComparingTo("0");

        UUID r = draftReturnLines(customer, invoice.getId(), "100000", p, "1");
        goodsReturnService.confirmReturn(r, BRANCH, USER);

        assertThat(jdbc.queryForObject("SELECT status FROM goods_return WHERE id = ?", String.class, r)).isEqualTo("CONFIRMED");
        assertThat(jdbc.queryForObject("SELECT refund_amount FROM goods_return WHERE id = ?", BigDecimal.class, r))
                .isEqualByComparingTo("100000");
        assertThat(debtService.getCurrentDebt(customer, BRANCH)).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM receivable_debt_movement WHERE ref_type = 'GOODS_RETURN' AND ref_id = ?
                """, Integer.class, r.toString())).as("không ghi biến động công nợ 0 đồng").isEqualTo(0);
        assertInventoryConsistent(p, "9");
    }

    @Test
    @DisplayName("Câu DELETE thu hồi idempotency chỉ xóa bản ghi IN_PROGRESS quá hạn, cùng hash")
    void idempotency_deleteStaleInProgress_onPostgres() {
        String stale = "it-stale-" + UUID.randomUUID();
        String fresh = "it-fresh-" + UUID.randomUUID();
        String done = "it-done-" + UUID.randomUUID();
        transactionTemplate.executeWithoutResult(s -> {
            idempotencyRepo.save(record(stale, "h", "IN_PROGRESS", LocalDateTime.now().minusHours(1)));
            idempotencyRepo.save(record(fresh, "h", "IN_PROGRESS", LocalDateTime.now()));
            idempotencyRepo.save(record(done, "h", "{\"ok\":true}", LocalDateTime.now().minusHours(1)));
        });
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        Integer wrongHash = transactionTemplate.execute(s -> idempotencyRepo.deleteStaleInProgress(stale, "other", cutoff));
        Integer staleDeleted = transactionTemplate.execute(s -> idempotencyRepo.deleteStaleInProgress(stale, "h", cutoff));
        Integer freshDeleted = transactionTemplate.execute(s -> idempotencyRepo.deleteStaleInProgress(fresh, "h", cutoff));
        Integer doneDeleted = transactionTemplate.execute(s -> idempotencyRepo.deleteStaleInProgress(done, "h", cutoff));

        assertThat(wrongHash).isZero();
        assertThat(staleDeleted).isEqualTo(1);
        assertThat(freshDeleted).isZero();
        assertThat(doneDeleted).isZero();
    }

    private static IdempotencyRecord record(String key, String hash, String snapshot, LocalDateTime createdAt) {
        IdempotencyRecord r = new IdempotencyRecord();
        r.setIdempotencyKey(key);
        r.setRequestHash(hash);
        r.setResponseSnapshot(snapshot);
        r.setCreatedAt(createdAt);
        return r;
    }
}
