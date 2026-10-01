package com.storename.erp.analytics.api;

import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.test.MasterDataFixtures;
import com.storename.erp.test.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử end-to-end {@code GET /api/v1/analytics/stock-alerts} trên PostgreSQL thật.
 *
 * <p>Fixture đặt trên chi nhánh riêng {@link #BRANCH_ID} và dùng 4 sản phẩm của {@link MasterDataFixtures}
 * (SP 1..4 = id 9001..9004) để kiểm tra luôn phần làm giàu mã/tên sản phẩm qua {@code CatalogFacade}.
 * Kịch bản bao phủ đủ các quy tắc trong {@code docs/fix-dashboard/business-rules.md}:</p>
 * <ul>
 *   <li>SP 1: tồn -1, không cấu hình ngưỡng → NEGATIVE_STOCK (D-05).</li>
 *   <li>SP 2: tồn 5, ngưỡng 10 → LOW_STOCK.</li>
 *   <li>SP 3: tồn 20, ngưỡng 10 → không cảnh báo.</li>
 *   <li>SP 4: chưa có movement (tồn 0), ngưỡng 5 → LOW_STOCK.</li>
 * </ul>
 * Thứ tự mong đợi: NEGATIVE trước, sau đó theo tồn tăng dần → SP1, SP4 (0), SP2 (5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("postgres-it")
@Transactional
public class StockAlertApiPostgresIT extends PostgresIntegrationTest {

    private static final long BRANCH_ID = 902L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void getStockAlerts_shouldReturnNegativeThenLowStock_orderedByQuantity() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO branch (id, code, name, is_active, created_at, updated_at) VALUES (?, 'IT902', 'Chi nhánh IT 902', true, now(), now())",
                BRANCH_ID);
        MasterDataFixtures.ensureProducts(jdbcTemplate);

        insertStockMovement(MasterDataFixtures.PRODUCT_1, -1, "SALE");
        insertStockMovement(MasterDataFixtures.PRODUCT_2, 5, "INBOUND");
        insertStockMovement(MasterDataFixtures.PRODUCT_3, 20, "INBOUND");

        insertAlertConfig(MasterDataFixtures.PRODUCT_2, 10);
        insertAlertConfig(MasterDataFixtures.PRODUCT_3, 10);
        insertAlertConfig(MasterDataFixtures.PRODUCT_4, 5);

        mockMvc.perform(get("/api/v1/analytics/stock-alerts")
                        .param("branchId", String.valueOf(BRANCH_ID))
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.negativeCount").value(1))
                .andExpect(jsonPath("$.data.lowStockCount").value(2))
                .andExpect(jsonPath("$.data.alerts.length()").value(3))
                .andExpect(jsonPath("$.data.alerts[0].productId").value(MasterDataFixtures.PRODUCT_1))
                .andExpect(jsonPath("$.data.alerts[0].alertType").value("NEGATIVE_STOCK"))
                .andExpect(jsonPath("$.data.alerts[0].currentQuantity").value(-1))
                .andExpect(jsonPath("$.data.alerts[0].minQuantityThreshold").value(nullValue()))
                .andExpect(jsonPath("$.data.alerts[0].productCode").value(MasterDataFixtures.productCode(1)))
                .andExpect(jsonPath("$.data.alerts[0].branchName").value("Chi nhánh IT 902"))
                .andExpect(jsonPath("$.data.alerts[1].productId").value(MasterDataFixtures.PRODUCT_4))
                .andExpect(jsonPath("$.data.alerts[1].alertType").value("LOW_STOCK"))
                .andExpect(jsonPath("$.data.alerts[1].currentQuantity").value(0))
                .andExpect(jsonPath("$.data.alerts[2].productId").value(MasterDataFixtures.PRODUCT_2))
                .andExpect(jsonPath("$.data.alerts[2].alertType").value("LOW_STOCK"))
                .andExpect(jsonPath("$.data.alerts[2].currentQuantity").value(5))
                .andExpect(jsonPath("$.data.alerts[2].minQuantityThreshold").value(10));
    }

    @Test
    void getStockAlerts_withoutBranchId_shouldNotFail() throws Exception {
        // Case "Tất cả chi nhánh" (branchId = null) từng gây lỗi 500 ở dashboard.
        mockMvc.perform(get("/api/v1/analytics/stock-alerts")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /** Admin HQ: không gắn chi nhánh (branchId = null trong JWT). */
    private UsernamePasswordAuthenticationToken adminAuth() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "b6a4a984-7dc4-4d23-95b8-c3dc164a2c5f", null, List.of(new SimpleGrantedAuthority("ADMIN")));
        auth.setDetails(new JwtAuthDetails(null, "it-token"));
        return auth;
    }

    private void insertStockMovement(long productId, int quantity, String movementType) {
        jdbcTemplate.update("""
                INSERT INTO stock_movement (id, product_id, branch_id, movement_type, quantity, ref_type, ref_id)
                VALUES (?, ?, ?, ?, ?, 'it_test', 'IT-902')
                """, UUID.randomUUID(), productId, BRANCH_ID, movementType, quantity);
    }

    private void insertAlertConfig(long productId, int threshold) {
        jdbcTemplate.update("""
                INSERT INTO inventory_alert_config (product_id, branch_id, min_quantity_threshold, is_active, created_at, updated_at)
                VALUES (?, ?, ?, true, now(), now())
                """, productId, BRANCH_ID, threshold);
    }
}
