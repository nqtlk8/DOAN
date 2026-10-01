package com.storename.erp.test;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra dữ liệu hệ thống chỉ-HQ ({@code db/migration-hq/R__reference_data.sql}) trên PostgreSQL thật:
 * chạy Flyway giống profile HQ (db/migration + db/migration-hq) trong một schema riêng để không ảnh hưởng
 * các test khác dùng chung container.
 */
class ReferenceDataPostgresIT extends PostgresIntegrationTest {

    private static final String SCHEMA = "hq_refdata_it";
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrateLikeHq() {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(SCHEMA)
                .locations("classpath:db/migration", "classpath:db/migration-hq")
                .load();
        flyway.migrate();
        // Chạy lại lần 2: repeatable migration không đổi checksum -> không chạy lại, dữ liệu giữ nguyên.
        flyway.migrate();

        DriverManagerDataSource ds = new DriverManagerDataSource(
                postgres.getJdbcUrl() + "&currentSchema=" + SCHEMA, postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(ds);
    }

    @Test
    void categoryTree_matchesWebPublicCategories() {
        List<Map<String, Object>> roots = jdbc.queryForList(
                "SELECT code, name FROM category WHERE parent_id IS NULL ORDER BY id");
        assertThat(roots).extracting(r -> r.get("code")).containsExactly("vlxd", "ttnt");
        assertThat(roots).extracting(r -> r.get("name")).containsExactly("VẬT LIỆU XÂY DỰNG", "TRANG TRÍ NỘI THẤT");

        List<String> vlxdChildren = jdbc.queryForList(
                "SELECT c.code FROM category c JOIN category p ON p.id = c.parent_id WHERE p.code = 'vlxd' ORDER BY c.id",
                String.class);
        assertThat(vlxdChildren).containsExactly("ban-cau", "bon-cau-1-khoi", "bon-cau-2-khoi", "bon-cau-thong-minh", "chau-rua");

        List<String> ttntChildren = jdbc.queryForList(
                "SELECT c.code FROM category c JOIN category p ON p.id = c.parent_id WHERE p.code = 'ttnt' ORDER BY c.id",
                String.class);
        assertThat(ttntChildren).containsExactly("den-trang-tri", "rem-cua", "sofa", "ban-tra", "do-trang-tri");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM category", Integer.class)).isEqualTo(12);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM category WHERE NOT is_active", Integer.class)).isZero();
    }

    @Test
    void systemData_branchesRolesAndAccountsExist() {
        assertThat(jdbc.queryForList("SELECT id || ':' || code FROM branch ORDER BY id", String.class))
                .containsExactly("1:TP1", "2:TP2");
        assertThat(jdbc.queryForList("SELECT code FROM role ORDER BY code", String.class))
                .containsExactly("ADMIN", "STAFF");
        assertThat(jdbc.queryForList(
                "SELECT u.username || ':' || r.code || ':' || coalesce(b.code, '-') FROM user_branch_role ubr "
                        + "JOIN user_account u ON u.id = ubr.user_id JOIN role r ON r.id = ubr.role_id "
                        + "LEFT JOIN branch b ON b.id = ubr.branch_id ORDER BY u.username", String.class))
                .containsExactly("admin:ADMIN:-", "staff_tp1:STAFF:TP1", "staff_tp2:STAFF:TP2");
    }
}
