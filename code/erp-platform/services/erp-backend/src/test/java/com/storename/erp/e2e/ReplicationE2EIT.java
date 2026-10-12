package com.storename.erp.e2e;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử E2E logical replication HQ -> chi nhánh trên hai PostgreSQL thật.
 *
 * <p>Không chạy trong {@code mvn verify} mặc định (bị bỏ qua khi thiếu cờ {@code e2e.enabled}).
 * Chạy bằng {@code scripts/test.ps1 e2e} hoặc {@code scripts/test.sh e2e}: script dựng
 * docker-compose trong {@code src/test/resources/replication-e2e}, tạo publication/subscription
 * rồi gọi {@code mvnw verify -Pe2e}.</p>
 */
@EnabledIfSystemProperty(named = "e2e.enabled", matches = "true")
public class ReplicationE2EIT {

    private static final Logger log = LoggerFactory.getLogger(ReplicationE2EIT.class);

    private static final String MASTER_URL = "jdbc:postgresql://localhost:5432/erp_db";
    private static final String REPLICA_URL = "jdbc:postgresql://localhost:5433/erp_db";
    private static final String USER = "erp_user";
    private static final String PASS = "erp_password";
    private static final String TEST_CODE = "TEST_REP";
    private static final int MAX_WAIT_SECONDS = 30;

    @Test
    public void categoryInsertedOnMasterIsReplicatedToReplica() throws Exception {
        try (Connection master = DriverManager.getConnection(MASTER_URL, USER, PASS);
             Statement stmt = master.createStatement()) {
            log.info("Thêm danh mục {} vào master", TEST_CODE);
            stmt.executeUpdate("INSERT INTO category (code, name, is_active) VALUES ('" + TEST_CODE
                    + "', 'Replication Test', true) ON CONFLICT (code) DO NOTHING");
        }

        String replicatedName = waitForReplicaName();
        assertEquals("Replication Test", replicatedName);
    }

    /** Poll replica tới khi thấy dòng (độ trễ logical replication thay đổi theo máy). */
    private String waitForReplicaName() throws Exception {
        for (int i = 0; i < MAX_WAIT_SECONDS; i++) {
            try (Connection replica = DriverManager.getConnection(REPLICA_URL, USER, PASS);
                 Statement stmt = replica.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM category WHERE code = '" + TEST_CODE + "'")) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
            Thread.sleep(1000);
        }
        assertTrue(false, "Danh mục ghi trên master chưa xuất hiện ở replica sau " + MAX_WAIT_SECONDS + "s");
        return null;
    }
}
