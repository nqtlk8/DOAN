package com.storename.erp;

import com.storename.erp.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("branch")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:branch_arch_db",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
public class ArchitectureV3BranchTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    public void branch_ShouldNotHaveRedisAndNoLogin() throws Exception {
        // 1. Redis MUST NOT exist
        assertFalse(context.containsBean("redisTemplate"), "Branch MUST NOT have RedisTemplate bean");

        // 2. AuthController MUST NOT exist
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"username\": \"admin\", \"password\": \"admin\"}"))
                .andExpect(status().isNotFound()); // 404, not 401
    }

    @Test
    public void branch_ShouldVerifyJwtLocally() throws Exception {
        // 1. Generate a test RSA KeyPair
        java.security.KeyPairGenerator keyPairGen = java.security.KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        java.security.KeyPair pair = keyPairGen.generateKeyPair();

        // 2. Inject the public key into the Branch's JwtTokenProvider bean using Reflection
        org.springframework.test.util.ReflectionTestUtils.setField(jwtTokenProvider, "publicKey", pair.getPublic());
        org.springframework.test.util.ReflectionTestUtils.setField(jwtTokenProvider, "expirationMs", 3600000L);

        // 3. Act as HQ: Generate a token using the private key
        // We temporarily set the private key to generate, then remove it to simulate Branch state
        org.springframework.test.util.ReflectionTestUtils.setField(jwtTokenProvider, "privateKey", pair.getPrivate());
        String token = jwtTokenProvider.generateToken("admin", "ADMIN", "HCM01", "token-1");
        org.springframework.test.util.ReflectionTestUtils.setField(jwtTokenProvider, "privateKey", null); // Remove private key again!

        // 4. Verify that without token, access to protected resource is 403 Forbidden (default Spring Security behavior)
        mockMvc.perform(get("/api/v1/catalog/products"))
               .andExpect(status().isForbidden());

        // 5. Verify that WITH token, access is allowed (might be 200 OK or 404 Not Found if endpoint is disabled, but NOT 403)
        mockMvc.perform(get("/api/v1/catalog/products")
               .header("Authorization", "Bearer " + token))
               .andExpect(result -> {
                   int statusCode = result.getResponse().getStatus();
                   org.junit.jupiter.api.Assertions.assertNotEquals(403, statusCode, "Should not be forbidden with a valid token");
               });
    }

    @Test
    public void branch_ShouldNotHaveHqOnlyBeans() {
        assertFalse(context.containsBean("customerWriteController"), "Branch must NOT have CustomerWriteController");
        assertFalse(context.containsBean("branchController"), "Branch must NOT have BranchController");
        assertFalse(context.containsBean("dashboardController"), "Branch must NOT have DashboardController");
    }
}
