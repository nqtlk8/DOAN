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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("branch")
@TestPropertySource(properties = {
    "instance.role=BRANCH",
    "spring.datasource.url=jdbc:h2:mem:branch_arch_db",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
public class ReplicationOwnershipTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Test
    @org.springframework.security.test.context.support.WithMockUser(authorities = "STAFF")
    public void branchCannotWriteProduct() throws Exception {
        // ProductWriteController and ProductWriter should NOT exist in Branch
        assertFalse(context.containsBean("productWriteController"), "Branch CANNOT have ProductWriteController");
        assertFalse(context.containsBean("productWriter"), "Branch CANNOT have ProductWriter");

        // The endpoint should be physically absent (404)
        mockMvc.perform(post("/api/v1/products")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json")
                .content("{\"code\":\"TEST\",\"name\":\"TEST\"}"))
               .andExpect(status().isNotFound());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(authorities = "STAFF")
    public void branchCanWriteSalesInvoice() throws Exception {
        assertTrue(context.containsBean("salesInvoiceController"), "Branch MUST have SalesInvoiceController");
        assertTrue(context.containsBean("salesInvoiceService"), "Branch MUST have SalesInvoiceService");

        // We provide empty body, which will trigger 400 Bad Request because of @Valid, 
        // proving the endpoint physically exists and was mapped.
        mockMvc.perform(post("/api/v1/sales-invoices")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/json")
                .content("{}"))
               .andExpect(status().isBadRequest());
    }
}
