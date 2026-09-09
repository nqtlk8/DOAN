package com.storename.erp.crm.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.common.security.JwtTokenProvider;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.crm.domain.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles({"test"})
@TestPropertySource(properties = {
    "instance.role=HQ"
})
public class CustomerCrudTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepo;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String authToken;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        // Change role to ADMIN to allow create
        authToken = "Bearer " + jwtTokenProvider.generateToken(UUID.randomUUID().toString(), "ADMIN", "HQ", UUID.randomUUID().toString());
        
        testCustomer = new Customer();
        testCustomer.setCustomerCode("CUS-001");
        testCustomer.setName("Test Customer");
        testCustomer.setPhone("0987654321");
        customerRepo.save(testCustomer);
    }

    @Test
    void createCustomer_noType_succeeds() throws Exception {
        String payload = """
                {
                    "customerCode": "CUS-002",
                    "name": "New Customer",
                    "phone": "0123456789"
                }
                """;

        mockMvc.perform(post("/api/v1/customers")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.customerType").doesNotExist());
    }

    @Test
    void getCustomers_responseNoType() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Test Customer"))
                .andExpect(jsonPath("$.data[0].customerType").doesNotExist());
    }
}

