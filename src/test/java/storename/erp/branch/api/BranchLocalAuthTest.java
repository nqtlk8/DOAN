package com.storename.erp.branch.api;

import com.storename.erp.common.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.lang.reflect.Field;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "instance.role=BRANCH",
        "instance.branch-id=BR01",
        "jwt.public-key=classpath:certs/public_key.pem",
        "spring.datasource.url=jdbc:h2:mem:testdb_${random.uuid};DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
public class BranchLocalAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    void testBranchVerifiesJwtLocally() throws Exception {
        JwtTokenProvider hqProvider = new JwtTokenProvider(
                new org.springframework.core.io.ClassPathResource("certs/private_key.pem"),
                new org.springframework.core.io.ClassPathResource("certs/public_key.pem"));
        
        Field field = JwtTokenProvider.class.getDeclaredField("expirationMs");
        field.setAccessible(true);
        field.set(hqProvider, 1800000L);
        
        String validToken = hqProvider.generateToken("branch_user", "SALES", "BR01", UUID.randomUUID().toString());

        mockMvc.perform(get("/api/branches/1")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden()); // expects 403 because branchId BR01 doesn't match ID 1
    }
}
