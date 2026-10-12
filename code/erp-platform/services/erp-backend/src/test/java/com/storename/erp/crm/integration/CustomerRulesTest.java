package com.storename.erp.crm.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.branch.domain.Branch;
import com.storename.erp.branch.infrastructure.BranchRepository;
import com.storename.erp.common.security.JwtTokenProvider;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Nhóm 2 — Khách hàng: trùng mã, kiểm tra branchId, tên khóa JSON (instance HQ). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "instance.role=HQ")
class CustomerRulesTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private BranchRepository branchRepo;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtTokenProvider.generateToken(
                UUID.randomUUID().toString(), "ADMIN", "HQ", UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("TC-C01: tạo khách trùng mã -> 409 CUSTOMER_CODE_EXISTS")
    void createCustomer_duplicateCode_returns409() throws Exception {
        newCustomer("TC-C01", false);

        create(Map.of("customerCode", "TC-C01", "name", "Khách khác"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("CUSTOMER_CODE_EXISTS"));
    }

    @Test
    @DisplayName("TC-C02: tạo khách trùng mã của khách đã xóa mềm -> 409")
    void createCustomer_codeOfSoftDeletedCustomer_returns409() throws Exception {
        newCustomer("TC-C02", true);

        create(Map.of("customerCode", "TC-C02", "name", "Khách mới"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("CUSTOMER_CODE_EXISTS"));
    }

    @Test
    @DisplayName("TC-C03: tạo khách với branchId không tồn tại -> 400")
    void createCustomer_unknownBranch_returns400() throws Exception {
        create(Map.of("customerCode", "TC-C03", "name", "Khách", "branchId", 987654))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("TC-C04: tạo khách với branchId có thật -> 201")
    void createCustomer_existingBranch_succeeds() throws Exception {
        Branch branch = branchRepo.saveAndFlush(Branch.builder().code("TC-C04").name("CN test").isActive(true).build());

        create(Map.of("customerCode", "TC-C04", "name", "Khách", "branchId", branch.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.branchId").value(branch.getId().intValue()));
    }

    @Test
    @DisplayName("TC-C05: sửa khách sang branchId không tồn tại -> 400")
    void updateCustomer_unknownBranch_returns400() throws Exception {
        Customer c = newCustomer("TC-C05", false);

        mockMvc.perform(put("/api/v1/customers/{id}", c.getId()).header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("branchId", 987654))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-C06: JSON khách hàng dùng khóa isDeleted, không có khóa deleted")
    void customerJson_usesIsDeletedKey() throws Exception {
        newCustomer("TC-C06", false);

        mockMvc.perform(get("/api/v1/customers").param("search", "TC-C06").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].customerCode").value("TC-C06"))
                .andExpect(jsonPath("$.data[0].isDeleted").value(false))
                .andExpect(jsonPath("$.data[0].deleted").doesNotExist());
    }

    private org.springframework.test.web.servlet.ResultActions create(Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/customers").header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }

    private Customer newCustomer(String code, boolean deleted) {
        Customer c = new Customer();
        c.setCustomerCode(code);
        c.setName("Khách " + code);
        c.setDeleted(deleted);
        return customerRepo.saveAndFlush(c);
    }
}
