package com.storename.erp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.ApplicationContext;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.storename.erp.common.security.JwtAuthDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("hq")
@TestPropertySource(properties = {
    "instance.role=HQ",
    "spring.datasource.url=jdbc:h2:mem:hq_arch_db",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
public class ReplicationOwnershipHqTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Test
    public void hqCanWriteProduct() throws Exception {
        assertTrue(context.containsBean("productWriteController"), "HQ MUST have ProductWriteController");
        
        // Use MockUser with STAFF authority to pass authorization
        // branchId = null, tokenId = "token123"
        JwtAuthDetails details = new JwtAuthDetails(null, "token123");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "b6a4a984-7dc4-4d23-95b8-c3dc164a2c5f", null, Collections.singletonList(new SimpleGrantedAuthority("STAFF")));
        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Send invalid body -> 400 Bad Request, proves endpoint exists and accepts request
        mockMvc.perform(post("/api/v1/catalog/products")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json")
                .content("{}"))
               .andExpect(status().isBadRequest());
    }

    @Test
    public void hqCannotWriteSalesInvoice() throws Exception {
        // HQ users don't have branchId
        JwtAuthDetails details = new JwtAuthDetails(null, "token123");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "b6a4a984-7dc4-4d23-95b8-c3dc164a2c5f", null, Collections.singletonList(new SimpleGrantedAuthority("STAFF")));
        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // SalesInvoiceController is now annotated with @ConditionalOnProperty(name="instance.role", havingValue="BRANCH")
        // So the endpoint will physically not exist in HQ, returning 404 Not Found.
        mockMvc.perform(post("/api/v1/sales-invoices")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isNotFound());
    }
}
