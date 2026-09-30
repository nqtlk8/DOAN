package com.storename.erp.analytics.infrastructure;

import com.storename.erp.analytics.api.dto.ProductPerformanceDto;
import com.storename.erp.analytics.domain.SalesTotals;
import com.storename.erp.analytics.domain.StockLevel;
import com.storename.erp.test.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử {@link AnalyticsDataAdapter} trên PostgreSQL thật (Testcontainers + Flyway V1..V21).
 *
 * <p>Vì sao không dùng H2: lỗi 500 của dashboard ("could not determine data type of parameter $1")
 * chỉ xảy ra trên PostgreSQL khi tham số {@code branchId} là null không có kiểu; H2 bỏ qua lỗi này.</p>
 *
 * <p>DB test đã có seed V2/V13 (branch 1, 2; product 1..6; customer KH-001..003). Để assert số tuyệt đối,
 * mỗi test tạo dữ liệu trên một chi nhánh riêng ({@link #BRANCH_ID}) không có dữ liệu seed.
 * Với truy vấn "tất cả chi nhánh" (branchId = null), test so sánh chênh lệch trước/sau khi thêm fixture.
 * Lớp chạy trong {@code @Transactional} nên mọi fixture được rollback sau mỗi test.</p>
 */
@SpringBootTest
@ActiveProfiles("postgres-it")
@Transactional
public class AnalyticsDataAdapterPostgresIT extends PostgresIntegrationTest {

    /** Chi nhánh chỉ dùng cho test, không có dữ liệu seed. */
    private static final long BRANCH_ID = 901L;
    /** Product seed V2 (SP-G001), dùng lại để không phải tạo category/product. */
    private static final long PRODUCT_ID = 1L;
    /** Customer seed V2 (KH-001). */
    private static final String SEED_CUSTOMER_ID = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11";

    private static final LocalDateTime FROM_TS = LocalDateTime.of(2026, 9, 1, 0, 0);
    private static final LocalDateTime TO_TS = LocalDateTime.of(2026, 9, 28, 0, 0);

    @Autowired
    private AnalyticsDataAdapter analyticsDataAdapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void createTestBranch() {
        jdbcTemplate.update(
                "INSERT INTO branch (id, code, name, is_active, created_at, updated_at) VALUES (?, 'IT901', 'Chi nhánh IT 901', true, now(), now())",
                BRANCH_ID);
    }

    @Test
    void salesQueries_shouldCountOnlyConfirmedInvoicesInPeriod_forBranchAndForAllBranches() {
        // Mốc "tất cả chi nhánh" trước khi thêm fixture (DB có sẵn seed V13).
        SalesTotals allBefore = analyticsDataAdapter.getSalesTotals(null, FROM_TS, TO_TS);

        // Hóa đơn CONFIRMED trong kỳ: 2 x 100 = 200 doanh thu, giá vốn 2 x 60 = 120.
        insertInvoiceWithLine("IT-HD-001", "CONFIRMED", LocalDateTime.of(2026, 9, 10, 9, 0), 2, 100, 60);
        // Hóa đơn CANCELLED trong kỳ -> không được tính.
        insertInvoiceWithLine("IT-HD-002", "CANCELLED", LocalDateTime.of(2026, 9, 11, 9, 0), 5, 100, 60);
        // Hóa đơn CONFIRMED ngoài kỳ (đúng mốc toTs, khoảng mở bên phải) -> không được tính.
        insertInvoiceWithLine("IT-HD-003", "CONFIRMED", TO_TS, 7, 100, 60);

        // Theo chi nhánh
        SalesTotals branchTotals = analyticsDataAdapter.getSalesTotals(BRANCH_ID, FROM_TS, TO_TS);
        assertThat(branchTotals.getRevenue()).isEqualByComparingTo("200");
        assertThat(branchTotals.getCogs()).isEqualByComparingTo("120");

        List<ProductPerformanceDto> top = analyticsDataAdapter.getTopProductsByRevenue(BRANCH_ID, FROM_TS, TO_TS, 10);
        assertThat(top).hasSize(1);
        assertThat(top.get(0).getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(top.get(0).getQuantitySold()).isEqualByComparingTo("2");
        assertThat(top.get(0).getRevenue()).isEqualByComparingTo("200");

        assertThat(analyticsDataAdapter.getProductIdsSoldInPeriod(BRANCH_ID, FROM_TS, TO_TS)).containsExactly(PRODUCT_ID);

        // Tất cả chi nhánh (branchId = null) — đây là case gây lỗi 500 trước khi sửa.
        SalesTotals allAfter = analyticsDataAdapter.getSalesTotals(null, FROM_TS, TO_TS);
        assertThat(allAfter.getRevenue().subtract(allBefore.getRevenue())).isEqualByComparingTo("200");
        assertThat(allAfter.getCogs().subtract(allBefore.getCogs())).isEqualByComparingTo("120");
        assertThat(analyticsDataAdapter.getTopProductsByRevenue(null, FROM_TS, TO_TS, 10)).isNotEmpty();
        assertThat(analyticsDataAdapter.getProductIdsSoldInPeriod(null, FROM_TS, TO_TS)).contains(PRODUCT_ID);
    }

    @Test
    void getInventoryValue_shouldSumRemainingQtyTimesUnitCost() {
        BigDecimal allBefore = analyticsDataAdapter.getInventoryValue(null);

        insertCostLayer(3, 50);   // 150
        insertCostLayer(0, 999);  // lô đã dùng hết -> 0

        assertThat(analyticsDataAdapter.getInventoryValue(BRANCH_ID)).isEqualByComparingTo("150");
        assertThat(analyticsDataAdapter.getInventoryValue(null).subtract(allBefore)).isEqualByComparingTo("150");
    }

    @Test
    void getTotalReceivableDebt_shouldIgnoreNegativeAndDeletedBalances() {
        BigDecimal allBefore = analyticsDataAdapter.getTotalReceivableDebt(null);

        insertReceivableDebt(insertCustomer("IT-KH-1"), 5000, false);  // tính
        insertReceivableDebt(insertCustomer("IT-KH-2"), -2000, false); // số dư âm (trả trước) -> bỏ qua
        insertReceivableDebt(insertCustomer("IT-KH-3"), 1000, true);   // đã xoá -> bỏ qua

        assertThat(analyticsDataAdapter.getTotalReceivableDebt(BRANCH_ID)).isEqualByComparingTo("5000");
        assertThat(analyticsDataAdapter.getTotalReceivableDebt(null).subtract(allBefore)).isEqualByComparingTo("5000");
    }

    @Test
    void getStockLevels_shouldAggregateMovementsPerProductAndBranch() {
        insertStockMovement(10, "INBOUND");
        insertStockMovement(-3, "SALE");
        insertStockMovement(2, "RETURN");

        List<StockLevel> levels = analyticsDataAdapter.getStockLevels(BRANCH_ID);
        assertThat(levels).hasSize(1);
        assertThat(levels.get(0).getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(levels.get(0).getBranchId()).isEqualTo(BRANCH_ID);
        assertThat(levels.get(0).getQuantity()).isEqualByComparingTo("9");

        StockLevel fromAll = analyticsDataAdapter.getStockLevels(null).stream()
                .filter(l -> l.getBranchId() == BRANCH_ID && l.getProductId() == PRODUCT_ID)
                .findFirst().orElseThrow();
        assertThat(fromAll.getQuantity()).isEqualByComparingTo("9");
    }

    // ---------------------------------------------------------------- fixtures

    private void insertInvoiceWithLine(String code, String status, LocalDateTime confirmedAt,
                                       int qty, int unitPrice, int unitCost) {
        UUID invoiceId = UUID.randomUUID();
        BigDecimal lineTotal = BigDecimal.valueOf((long) qty * unitPrice);
        jdbcTemplate.update("""
                INSERT INTO sales_invoice (id, invoice_code, customer_id, branch_id, total_amount, previous_debt,
                    remaining_debt, status, payment_method, version, is_deleted, created_at, updated_at, confirmed_at)
                VALUES (?, ?, ?::uuid, ?, ?, 0, 0, ?, 'CASH', 0, false, now(), now(), ?)
                """, invoiceId, code, SEED_CUSTOMER_ID, BRANCH_ID, lineTotal, status, Timestamp.valueOf(confirmedAt));
        jdbcTemplate.update("""
                INSERT INTO sales_invoice_line (id, invoice_id, product_id, product_name, quantity, unit_price,
                    unit_cost, line_total, unit_of_measure, version, is_deleted, created_at, updated_at)
                VALUES (?, ?, ?, 'SP test', ?, ?, ?, ?, 'Hộp', 0, false, now(), now())
                """, UUID.randomUUID(), invoiceId, PRODUCT_ID, qty, unitPrice, unitCost, lineTotal);
    }

    private void insertCostLayer(int remainingQty, int unitCost) {
        jdbcTemplate.update("""
                INSERT INTO cost_layer (id, product_id, branch_id, unit_cost, initial_qty, remaining_qty)
                VALUES (?, ?, ?, ?, 10, ?)
                """, UUID.randomUUID(), PRODUCT_ID, BRANCH_ID, unitCost, remainingQty);
    }

    private UUID insertCustomer(String code) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO customer (id, customer_code, name, is_deleted, version, created_at, updated_at)
                VALUES (?, ?, ?, false, 0, now(), now())
                """, id, code, "Khách " + code);
        return id;
    }

    private void insertReceivableDebt(UUID customerId, int totalDebt, boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO receivable_debt (id, customer_id, branch_id, total_debt, version, is_deleted, created_at, updated_at)
                VALUES (?, ?, ?, ?, 0, ?, now(), now())
                """, UUID.randomUUID(), customerId, BRANCH_ID, totalDebt, deleted);
    }

    private void insertStockMovement(int quantity, String movementType) {
        jdbcTemplate.update("""
                INSERT INTO stock_movement (id, product_id, branch_id, movement_type, quantity, ref_type, ref_id)
                VALUES (?, ?, ?, ?, ?, 'it_test', 'IT-901')
                """, UUID.randomUUID(), PRODUCT_ID, BRANCH_ID, movementType, quantity);
    }
}
