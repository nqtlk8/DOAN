package com.storename.erp.catalog;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E Test for PostgreSQL Logical Replication.
 *
 * To run this test:
 * 1. Start the Docker containers:
 *    cd src/test/resources/replication-e2e
 *    docker-compose up -d
 * 2. Ensure both databases are up and schema is initialized.
 * 3. Setup logical replication via SQL:
 *    Master: CREATE PUBLICATION erp_pub FOR ALL TABLES;
 *    Replica: CREATE SUBSCRIPTION erp_sub CONNECTION 'host=pg-master port=5432 user=erp_user password=erp_password dbname=erp_db' PUBLICATION erp_pub;
 * 4. Run this test.
 */
@Disabled("Manual E2E Test requiring running Docker containers")
public class ReplicationE2ETest {

    private static final Logger log = LoggerFactory.getLogger(ReplicationE2ETest.class);

    private static final String MASTER_URL = "jdbc:postgresql://localhost:5432/erp_db";
    private static final String REPLICA_URL = "jdbc:postgresql://localhost:5433/erp_db";
    private static final String USER = "erp_user";
    private static final String PASS = "erp_password";

    @Test
    public void testLogicalReplicationSync() throws Exception {
        log.info("Connecting to Master DB...");
        try (Connection masterConn = DriverManager.getConnection(MASTER_URL, USER, PASS);
             Statement masterStmt = masterConn.createStatement()) {
             
            // We just test replication of the category table for simplicity
            log.info("Inserting category 'TEST_REP' into Master...");
            masterStmt.executeUpdate("INSERT INTO category (code, name, is_active) VALUES ('TEST_REP', 'Replication Test', true) ON CONFLICT (code) DO NOTHING");
        }

        // Wait for replication lag
        log.info("Waiting 2 seconds for logical replication to sync...");
        Thread.sleep(2000);

        log.info("Connecting to Replica DB...");
        boolean foundInReplica = false;
        try (Connection replicaConn = DriverManager.getConnection(REPLICA_URL, USER, PASS);
             Statement replicaStmt = replicaConn.createStatement()) {
             
            log.info("Querying category 'TEST_REP' from Replica...");
            try (ResultSet rs = replicaStmt.executeQuery("SELECT name FROM category WHERE code = 'TEST_REP'")) {
                if (rs.next()) {
                    foundInReplica = true;
                    String name = rs.getString("name");
                    assertEquals("Replication Test", name);
                    log.info("Successfully read replicated data from Replica!");
                }
            }
        }

        assertTrue(foundInReplica, "Category inserted in Master should be readable from Replica");
    }
}
