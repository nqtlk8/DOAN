package com.storename.erp.replication;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử replicate bảng snapshot {@code stock_on_hand}, {@code receivable_debt} từ Branch lên HQ
 * bằng publication có row filter {@code WHERE (branch_id = N)} (ADR-13, sprint FD-2).
 *
 * <p>Dựng 2 container PostgreSQL 15 (cùng major version với docker-compose), chạy Flyway V1..V21 trên cả hai.
 * Cả hai DB đều có seed V2/V13 với dữ liệu {@code branch_id = 1} — đúng tình huống thực tế khiến phải lọc dòng.
 * Container "branch" đóng vai chi nhánh TP2 ({@code branch_id = 2}).</p>
 *
 * <p>Điều được khẳng định:</p>
 * <ol>
 *   <li>Dữ liệu có sẵn của chi nhánh được copy ban đầu ({@code copy_data = true}).</li>
 *   <li>INSERT/UPDATE dòng {@code branch_id = 2} được đẩy lên HQ (UPDATE chỉ chạy được nhờ V21
 *       đặt {@code REPLICA IDENTITY USING INDEX}).</li>
 *   <li>Dòng {@code branch_id = 1} (seed) ở chi nhánh KHÔNG được đẩy lên HQ — kể cả INSERT lẫn UPDATE.</li>
 * </ol>
 */
@Testcontainers
public class SnapshotReplicationPostgresIT {

    private static final Logger log = LoggerFactory.getLogger(SnapshotReplicationPostgresIT.class);
    private static final Network NETWORK = Network.newNetwork();

    /** Chi nhánh giả lập (TP2 trong seed V2). */
    private static final long BRANCH_ID = 2L;
    /** Customer seed V2 (KH-002) — có ở cả hai DB vì seed master chạy trên mọi instance. */
    private static final String SEED_CUSTOMER_ID = "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22";

    @Container
    private static final PostgreSQLContainer<?> HQ_DB = newDb("hq-db", "erp_hq");

    @Container
    private static final PostgreSQLContainer<?> BRANCH_DB = newDb("branch-db", "erp_branch_tp2");

    private static PostgreSQLContainer<?> newDb(String alias, String dbName) {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withNetwork(NETWORK)
                .withNetworkAliases(alias)
                .withCommand("postgres", "-c", "wal_level=logical")
                .withDatabaseName(dbName)
                .withUsername("erp_user")
                .withPassword("erp_pass");
    }

    @BeforeAll
    static void setUpReplication() throws Exception {
        migrate(HQ_DB);
        migrate(BRANCH_DB);

        // Dữ liệu có sẵn ở chi nhánh TRƯỚC khi bật replication -> phải được copy ban đầu.
        executeOn(BRANCH_DB, insertStockSql(6, BRANCH_ID, 42));

        // Giống enable-snapshot-replication.sh: publication có row filter ở chi nhánh.
        executeOn(BRANCH_DB, "CREATE PUBLICATION pub_tp2_to_hq FOR TABLE "
                + "stock_on_hand WHERE (branch_id = " + BRANCH_ID + "), "
                + "receivable_debt WHERE (branch_id = " + BRANCH_ID + ")");

        // HQ: chỉ xoá dòng của đúng chi nhánh này rồi subscribe với copy_data = true.
        executeOn(HQ_DB, "DELETE FROM stock_on_hand WHERE branch_id = " + BRANCH_ID);
        executeOn(HQ_DB, "DELETE FROM receivable_debt WHERE branch_id = " + BRANCH_ID);
        executeOn(HQ_DB, String.format(
                "CREATE SUBSCRIPTION sub_hq_from_tp2 CONNECTION 'host=branch-db port=5432 user=%s password=%s dbname=%s' "
                        + "PUBLICATION pub_tp2_to_hq WITH (copy_data = true)",
                BRANCH_DB.getUsername(), BRANCH_DB.getPassword(), BRANCH_DB.getDatabaseName()));
        log.info("Replication pub_tp2_to_hq -> sub_hq_from_tp2 created");
    }

    @AfterAll
    static void tearDown() {
        try {
            executeOn(HQ_DB, "DROP SUBSCRIPTION IF EXISTS sub_hq_from_tp2");
        } catch (Exception e) {
            log.warn("Could not drop subscription: {}", e.getMessage());
        }
        NETWORK.close();
    }

    @Test
    void replicaIdentity_shouldUseUniqueIndex_onBothSides() throws Exception {
        for (PostgreSQLContainer<?> db : new PostgreSQLContainer<?>[]{HQ_DB, BRANCH_DB}) {
            assertThat(queryString(db, "SELECT relreplident::text FROM pg_class WHERE relname = 'stock_on_hand'")).isEqualTo("i");
            assertThat(queryString(db, "SELECT relreplident::text FROM pg_class WHERE relname = 'receivable_debt'")).isEqualTo("i");
        }
    }

    @Test
    void stockOnHand_shouldReplicateOnlyOwnBranchRows() throws Exception {
        String hqStockSql = "SELECT quantity FROM stock_on_hand WHERE product_id = %d AND branch_id = %d";

        // 1. Copy ban đầu
        awaitDecimal(HQ_DB, String.format(hqStockSql, 6, BRANCH_ID), new BigDecimal("42"));

        // 2. INSERT dòng của chi nhánh 2
        executeOn(BRANCH_DB, insertStockSql(2, BRANCH_ID, 7));
        awaitDecimal(HQ_DB, String.format(hqStockSql, 2, BRANCH_ID), new BigDecimal("7"));

        // 3. UPDATE thành tồn âm
        executeOn(BRANCH_DB, "UPDATE stock_on_hand SET quantity = -3 WHERE product_id = 2 AND branch_id = " + BRANCH_ID);
        awaitDecimal(HQ_DB, String.format(hqStockSql, 2, BRANCH_ID), new BigDecimal("-3"));

        // 4. Dòng seed branch_id = 1 ở chi nhánh: UPDATE và INSERT đều không được lên HQ
        BigDecimal hqSeedBefore = queryDecimal(HQ_DB, String.format(hqStockSql, 1, 1));
        assertThat(hqSeedBefore).as("HQ phải có dòng seed V2 (product 1, branch 1)").isNotNull();
        executeOn(BRANCH_DB, "UPDATE stock_on_hand SET quantity = 9999 WHERE product_id = 1 AND branch_id = 1");
        executeOn(BRANCH_DB, insertStockSql(3, 1, 55));

        // Chờ một thay đổi của branch 2 đi qua sau đó -> mọi thay đổi trước nó đã được xử lý
        executeOn(BRANCH_DB, "UPDATE stock_on_hand SET quantity = -4 WHERE product_id = 2 AND branch_id = " + BRANCH_ID);
        awaitDecimal(HQ_DB, String.format(hqStockSql, 2, BRANCH_ID), new BigDecimal("-4"));

        assertThat(queryDecimal(HQ_DB, String.format(hqStockSql, 1, 1))).isEqualByComparingTo(hqSeedBefore);
        assertThat(queryDecimal(HQ_DB, String.format(hqStockSql, 3, 1))).isNull();
    }

    @Test
    void receivableDebt_shouldReplicateInsertAndUpdate() throws Exception {
        String hqDebtSql = "SELECT total_debt FROM receivable_debt WHERE customer_id = '" + SEED_CUSTOMER_ID
                + "' AND branch_id = " + BRANCH_ID;

        executeOn(BRANCH_DB, "INSERT INTO receivable_debt (id, customer_id, branch_id, total_debt, version, is_deleted, created_at, updated_at) "
                + "VALUES (gen_random_uuid(), '" + SEED_CUSTOMER_ID + "', " + BRANCH_ID + ", 5000, 0, false, now(), now())");
        awaitDecimal(HQ_DB, hqDebtSql, new BigDecimal("5000"));

        executeOn(BRANCH_DB, "UPDATE receivable_debt SET total_debt = 2000 WHERE customer_id = '" + SEED_CUSTOMER_ID
                + "' AND branch_id = " + BRANCH_ID);
        awaitDecimal(HQ_DB, hqDebtSql, new BigDecimal("2000"));
    }

    // ---------------------------------------------------------------- helpers

    private static String insertStockSql(long productId, long branchId, int quantity) {
        return "INSERT INTO stock_on_hand (id, product_id, branch_id, quantity, version, is_deleted, created_at, updated_at) "
                + "VALUES (gen_random_uuid(), " + productId + ", " + branchId + ", " + quantity + ", 0, false, now(), now())";
    }

    private static void migrate(PostgreSQLContainer<?> db) {
        // Chỉ dùng db/migration: V9 trong db/migration-branch cần role erp_user có sẵn quyền theo docker-compose.
        Flyway.configure()
                .dataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private static Connection connect(PostgreSQLContainer<?> db) throws SQLException {
        return DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
    }

    private static void executeOn(PostgreSQLContainer<?> db, String sql) throws SQLException {
        try (Connection c = connect(db); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    private static String queryString(PostgreSQLContainer<?> db, String sql) throws SQLException {
        try (Connection c = connect(db); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static BigDecimal queryDecimal(PostgreSQLContainer<?> db, String sql) throws SQLException {
        try (Connection c = connect(db); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : null;
        }
    }

    /** Chờ tối đa 15 giây cho tới khi HQ nhận đúng giá trị (replication là bất đồng bộ). */
    private static void awaitDecimal(PostgreSQLContainer<?> db, String sql, BigDecimal expected) throws Exception {
        BigDecimal actual = null;
        for (int i = 0; i < 30; i++) {
            actual = queryDecimal(db, sql);
            if (actual != null && actual.compareTo(expected) == 0) {
                return;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Replication timeout: expected " + expected + " but was " + Objects.toString(actual)
                + " for query: " + sql);
    }
}
