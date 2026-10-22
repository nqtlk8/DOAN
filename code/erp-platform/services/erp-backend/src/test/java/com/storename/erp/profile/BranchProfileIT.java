package com.storename.erp.profile;

import com.storename.erp.common.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ràng buộc vai trò chi nhánh: không có bean/endpoint HQ, không ghi được master data,
 * xác thực JWT cục bộ bằng public key.
 *
 * <p>Dùng một context chung cho profile branch (cùng H2) để tránh khởi động Spring hai lần.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("branch")
@TestPropertySource(properties = {
    "instance.role=BRANCH",
    "branch-id=HCM01",
    "spring.datasource.url=jdbc:h2:mem:branch_profile_db",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BranchProfileIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void branch_hasNoRedisAndNoLoginEndpoint() throws Exception {
        assertFalse(context.containsBean("redisTemplate"), "Chi nhánh KHÔNG được có RedisTemplate");

        // Không có endpoint đăng nhập -> 404
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"admin\", \"password\": \"admin\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void branch_verifiesJwtLocallyWithPublicKey() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair pair = keyPairGen.generateKeyPair();

        // Chi nhánh chỉ giữ public key. Tạm gán private key để "HQ" phát token, rồi gỡ đi.
        ReflectionTestUtils.setField(jwtTokenProvider, "publicKey", pair.getPublic());
        ReflectionTestUtils.setField(jwtTokenProvider, "expirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "privateKey", pair.getPrivate());
        String token = jwtTokenProvider.generateToken("admin", "ADMIN", "HCM01", "token-1");
        ReflectionTestUtils.setField(jwtTokenProvider, "privateKey", null);

        // Không token -> 403 (hành vi mặc định của Spring Security)
        mockMvc.perform(get("/api/v1/catalog/products"))
                .andExpect(status().isForbidden());

        // Có token hợp lệ -> không bị 403 (có thể 200 hoặc 404 tùy endpoint)
        mockMvc.perform(get("/api/v1/catalog/products")
                .header("Authorization", "Bearer " + token))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus(),
                        "Token hợp lệ không được bị 403"));
    }

    @Test
    void branch_hasNoHqOnlyBeans() {
        assertFalse(context.containsBean("customerWriteController"), "Chi nhánh KHÔNG được có CustomerWriteController");
        assertFalse(context.containsBean("branchController"), "Chi nhánh KHÔNG được có BranchController");
        assertFalse(context.containsBean("dashboardController"), "Chi nhánh KHÔNG được có DashboardController");
    }

    @Test
    @WithMockUser(authorities = "STAFF")
    void branchCannotWriteProduct() throws Exception {
        assertFalse(context.containsBean("productWriteController"), "Chi nhánh KHÔNG được có ProductWriteController");
        assertFalse(context.containsBean("productWriter"), "Chi nhánh KHÔNG được có ProductWriter");

        mockMvc.perform(post("/api/v1/products")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"TEST\",\"name\":\"TEST\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "STAFF")
    void branchCanWriteSalesInvoice() throws Exception {
        assertTrue(context.containsBean("salesInvoiceController"), "Chi nhánh PHẢI có SalesInvoiceController");
        assertTrue(context.containsBean("salesInvoiceService"), "Chi nhánh PHẢI có SalesInvoiceService");

        // Body rỗng -> 400 (@Valid): endpoint tồn tại và đã được map
        mockMvc.perform(post("/api/v1/sales-invoices")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
