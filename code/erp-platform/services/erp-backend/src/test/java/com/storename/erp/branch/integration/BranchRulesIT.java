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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Nhóm 2 — Chi nhánh: không xóa, không trùng mã, cập nhật từng phần, URL cổng (instance HQ). */
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
    @DisplayName("TC-B01: không có API xóa chi nhánh -> DELETE trả 405, chi nhánh vẫn còn")
    void deleteBranch_isNotSupported() throws Exception {
        mockMvc.perform(delete("/api/v1/branches/{id}", existing.getId()).header("Authorization", globalAdminToken))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errors[0]").value("METHOD_NOT_ALLOWED"));
        assertTrue(branchRepo.existsById(existing.getId()));
    }

    @Test
    @DisplayName("TC-B02: tạo chi nhánh trùng mã -> 409 BRANCH_CODE_EXISTS")
    void createBranch_duplicateCode_returns409() throws Exception {
        create(Map.of("code", "TC-B-BASE", "name", "Trùng mã"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("BRANCH_CODE_EXISTS"));
    }

    @Test
    @DisplayName("TC-B03: tạo chi nhánh thiếu tên -> 400")
    void createBranch_missingName_returns400() throws Exception {
        create(Map.of("code", "TC-B03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_ARGUMENT"));
    }

    @Test
    @DisplayName("TC-B04: tạo chi nhánh không gửi isActive -> mặc định đang hoạt động, JSON dùng khóa isActive")
    void createBranch_defaultsActive_andUsesIsActiveKey() throws Exception {
        create(Map.of("code", "TC-B04", "name", "Chi nhánh mới"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.active").doesNotExist());
    }

    @Test
    @DisplayName("TC-B05: PUT chỉ gửi tên -> các trường khác giữ nguyên, chi nhánh không bị tắt")
    void updateBranch_partialBody_keepsOtherFields() throws Exception {
        update(Map.of("name", "Tên mới"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Tên mới"))
                .andExpect(jsonPath("$.data.code").value("TC-B-BASE"))
                .andExpect(jsonPath("$.data.address").value("12 Lê Lợi"))
                .andExpect(jsonPath("$.data.internalUrl").value("http://localhost:81"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("TC-B06: đổi mã chi nhánh thành mã của chi nhánh khác -> 409")
    void updateBranch_toAnotherBranchesCode_returns409() throws Exception {
        branchRepo.saveAndFlush(Branch.builder().code("TC-B06").name("Khác").isActive(true).build());

        update(Map.of("code", "TC-B06"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("BRANCH_CODE_EXISTS"));
    }

    @Test
    @DisplayName("TC-B07: URL cổng không phải http(s) -> 400; URL hợp lệ được lưu")
    void updateBranch_portalUrlIsValidated() throws Exception {
        update(Map.of("internalUrl", "ftp://localhost:81"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value("INVALID_ARGUMENT"));
        update(Map.of("internalUrl", "https://tp1.example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.internalUrl").value("https://tp1.example.com"));
    }

    private ResultActions create(Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/branches").header("Authorization", globalAdminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions update(Map<String, Object> body) throws Exception {
        return mockMvc.perform(put("/api/v1/branches/{id}", existing.getId()).header("Authorization", globalAdminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }
}
