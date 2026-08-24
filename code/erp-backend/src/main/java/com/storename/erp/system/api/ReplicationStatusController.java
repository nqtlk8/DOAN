package com.storename.erp.system.api;

import com.storename.erp.common.api.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import java.sql.Connection;

@RestController
@RequestMapping("/api/admin/system")
public class ReplicationStatusController {

    private final JdbcTemplate jdbcTemplate;

    public ReplicationStatusController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/replication-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String, Object>>> getReplicationStatus() {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            // Check if we are Master (pg_stat_replication) or Replica (pg_stat_subscription)
            List<Map<String, Object>> status;
            
            // Note: This is specific to PostgreSQL. If running in H2, it will throw an exception,
            // so we should handle it gracefully for our H2 tests.
            String url = conn.getMetaData().getURL();
            if (url.startsWith("jdbc:h2")) {
                return ApiResponse.success(List.of(Map.of("status", "Running on H2, replication not applicable")));
            }

            try {
                // Try to query pg_stat_replication (HQ / Master)
                status = jdbcTemplate.queryForList("SELECT * FROM pg_stat_replication");
                if (!status.isEmpty()) {
                    return ApiResponse.success(status, "Master replication status");
                }
            } catch (Exception e) {
                // Ignore if view doesn't exist or not master
            }

            try {
                // Try to query pg_stat_subscription (Branch / Replica)
                status = jdbcTemplate.queryForList("SELECT * FROM pg_stat_subscription");
                return ApiResponse.success(status, "Replica subscription status");
            } catch (Exception e) {
                // Ignore
            }

            return ApiResponse.success(List.of(), "No active replication found");
        } catch (Exception e) {
            return ApiResponse.error("Error retrieving replication status: " + e.getMessage(), null);
        }
    }
}
