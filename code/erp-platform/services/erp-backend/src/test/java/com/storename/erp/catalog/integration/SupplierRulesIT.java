package com.storename.erp.catalog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.catalog.domain.Supplier;
import com.storename.erp.catalog.infrastructure.SupplierRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Nhóm 2 — Nhà cung cấp: kiểm tra trùng mã và mã số thuế (instance HQ). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "instance.role=HQ")
class SupplierRulesIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SupplierRepository supplierRepo;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = "Bearer " + jwtTokenProvider.generateToken(
                UUID.randomUUID().toString(), "ADMIN", "HQ", UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("TC-S01: tạo NCC trùng mã -> 409 SUPPLIER_CODE_EXISTS")
    void createSupplier_duplicateCode_returns409() throws Exception {
        newSupplier("TC-S01", "0300000001");

        create(Map.of("code", "TC-S01", "name", "NCC khác"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("SUPPLIER_CODE_EXISTS"));
    }

    @Test
    @DisplayName("TC-S02: tạo NCC trùng mã số thuế -> 409 SUPPLIER_TAX_CODE_EXISTS")
    void createSupplier_duplicateTaxCode_returns409() throws Exception {
        newSupplier("TC-S02-A", "0300000002");

        create(Map.of("code", "TC-S02-B", "name", "NCC B", "taxCode", "0300000002"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("SUPPLIER_TAX_CODE_EXISTS"));
    }

    @Test
    @DisplayName("TC-S03: đổi mã NCC thành mã của NCC khác -> 409")
    void updateSupplier_toAnotherSuppliersCode_returns409() throws Exception {
        newSupplier("TC-S03-A", null);
        Supplier b = newSupplier("TC-S03-B", null);

        mockMvc.perform(put("/api/v1/suppliers/{id}", b.getId()).header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "TC-S03-A"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value("SUPPLIER_CODE_EXISTS"));
    }

    @Test
    @DisplayName("TC-S04: cập nhật NCC giữ nguyên mã và MST của chính nó -> 200")
    void updateSupplier_keepingOwnCodes_succeeds() throws Exception {
        Supplier a = newSupplier("TC-S04", "0300000004");

        mockMvc.perform(put("/api/v1/suppliers/{id}", a.getId()).header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("code", "TC-S04", "taxCode", "0300000004", "name", "Tên mới"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Tên mới"));
    }

    @Test
    @DisplayName("TC-S05: nhiều NCC không có mã số thuế vẫn tạo được")
    void createSupplier_withoutTaxCode_isAllowedRepeatedly() throws Exception {
        create(Map.of("code", "TC-S05-A", "name", "NCC A")).andExpect(status().isOk());
        create(Map.of("code", "TC-S05-B", "name", "NCC B")).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions create(Map<String, String> body) throws Exception {
        return mockMvc.perform(post("/api/v1/suppliers").header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }

    private Supplier newSupplier(String code, String taxCode) {
        Supplier s = new Supplier();
        s.setCode(code);
        s.setName("NCC " + code);
        s.setTaxCode(taxCode);
        s.setIsActive(true);
        return supplierRepo.saveAndFlush(s);
    }
}
