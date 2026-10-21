package com.storename.erp.branch.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.branch.domain.Branch;
import com.storename.erp.branch.infrastructure.BranchRepository;
import com.storename.erp.common.security.JwtTokenProvider;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Nhóm 2 — Chi nhánh: chỉ đọc, không có API tạo hoặc xóa (instance HQ). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "instance.role=HQ")
class BranchRulesIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BranchRepository branchRepo;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String globalAdminToken;
    private Branch existing;

    @BeforeEach
    void setUp() {
        globalAdminToken = "Bearer " + jwtTokenProvider.generateToken(
                UUID.randomUUID().toString(), "ADMIN", null, UUID.randomUUID().toString());
        existing = branchRepo.saveAndFlush(Branch.builder()
                .code("TC-B-BASE").name("Chi nhánh gốc").address("12 Lê Lợi")
                .internalUrl("http://localhost:81").isActive(true).build());
    }

    @Test
    @DisplayName("TC-B01: không có API xóa chi nhánh -> DELETE bị từ chối (4xx), chi nhánh vẫn còn")
    void deleteBranch_isNotSupported() throws Exception {
        mockMvc.perform(delete("/api/v1/branches/{id}", existing.getId()).header("Authorization", globalAdminToken))
                .andExpect(status().is4xxClientError());
        assertTrue(branchRepo.existsById(existing.getId()));
    }

    @Test
    @DisplayName("TC-B02: không có API tạo chi nhánh -> POST trả 405")
    void createBranch_isNotSupported() throws Exception {
        mockMvc.perform(post("/api/v1/branches").header("Authorization", globalAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "TC-B02", "name", "Mới"))))
                .andExpect(status().isMethodNotAllowed());
        assertTrue(branchRepo.findAll().stream().noneMatch(b -> "TC-B02".equals(b.getCode())));
    }
}
