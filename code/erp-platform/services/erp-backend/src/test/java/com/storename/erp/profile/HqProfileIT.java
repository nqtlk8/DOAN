package com.storename.erp.profile;

import com.storename.erp.common.security.JwtAuthDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ràng buộc vai trò HQ: đúng bean/endpoint của HQ và HQ không có API ghi dữ liệu thuộc chi nhánh.
 *
 * <p>Dùng một context chung cho profile hq (cùng H2) để tránh khởi động Spring hai lần.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("hq")
@TestPropertySource(properties = {
    "instance.role=HQ",
    "spring.datasource.url=jdbc:h2:mem:hq_profile_db",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HqProfileIT {

    private static final String HQ_USER_ID = "b6a4a984-7dc4-4d23-95b8-c3dc164a2c5f";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hq_hasRedisTemplateAndLoginEndpoint() throws Exception {
        assertTrue(context.containsBean("redisTemplate"), "HQ phải có RedisTemplate");

        // Có endpoint đăng nhập: sai thông tin -> 401 (không phải 404)
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"admin\", \"password\": \"admin\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hq_hasHqOnlyBeans() {
        assertTrue(context.containsBean("customerWriteController"), "HQ phải có CustomerWriteController");
        assertTrue(context.containsBean("branchController"), "HQ phải có BranchController");
        assertTrue(context.containsBean("dashboardController"), "HQ phải có DashboardController");
    }

    @Test
    void hqCanWriteProduct() throws Exception {
        assertTrue(context.containsBean("productWriteController"), "HQ phải có ProductWriteController");
        authenticateAs("STAFF");

        // Body rỗng -> 400 (@Valid): chứng minh endpoint tồn tại và nhận request
        mockMvc.perform(post("/api/v1/catalog/products")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hqCannotWriteSalesInvoice() throws Exception {
        // SalesInvoiceController chỉ bật ở BRANCH/ALL -> HQ không có endpoint
        authenticateAs("STAFF");

        mockMvc.perform(post("/api/v1/sales-invoices")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void hqCannotWriteReceivableDebtOpeningBalance() throws Exception {
        // Opening balance cần branchId trong JWT; HQ không có -> 403
        authenticateAs("ADMIN");

        mockMvc.perform(post("/api/v1/receivable-debts/opening-balance")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"11111111-1111-1111-1111-111111111111\",\"amount\":1000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void hqCannotWriteInventoryInbound() throws Exception {
        authenticateAs("ADMIN");

        mockMvc.perform(post("/api/v1/inventory/inbound")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());
    }

    private void authenticateAs(String authority) {
        JwtAuthDetails details = new JwtAuthDetails(null, "token123");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                HQ_USER_ID, null, Collections.singletonList(new SimpleGrantedAuthority(authority)));
        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
