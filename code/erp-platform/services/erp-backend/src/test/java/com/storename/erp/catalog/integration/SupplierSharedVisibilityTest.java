package com.storename.erp.catalog.integration;

import com.storename.erp.catalog.domain.Supplier;
import com.storename.erp.catalog.infrastructure.SupplierRepository;
import com.storename.erp.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Supplier là master data dùng chung do HQ tạo và replicate xuống mọi chi nhánh.
 * Kiểm tra trên một instance BRANCH: nhân viên chi nhánh phải thấy mọi supplier đang hoạt động
 * (không lọc theo chi nhánh), không thấy supplier đã ngừng hoạt động, và mở được chi tiết.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles({"branch", "test"})
@TestPropertySource(properties = {
    "instance.role=BRANCH",
    "branch-id=1001"
})
public class SupplierSharedVisibilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String staffToken;
    private Supplier activeA;
    private Supplier inactive;

    @BeforeEach
    void setUp() {
        supplierRepo.deleteAll();
        activeA = supplierRepo.save(newSupplier("SUP-A", "Nhà cung cấp A", true));
        supplierRepo.save(newSupplier("SUP-B", "Nhà cung cấp B", true));
        inactive = supplierRepo.save(newSupplier("SUP-X", "Nhà cung cấp đã ngừng", false));

        staffToken = "Bearer " + jwtTokenProvider.generateToken(
                UUID.randomUUID().toString(), "STAFF", "1001", UUID.randomUUID().toString());
    }

    @Test
    void staffSeesAllActiveSharedSuppliers() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers").header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].code", containsInAnyOrder("SUP-A", "SUP-B")));
    }

    @Test
    void staffCanOpenSharedSupplierDetail() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/" + activeA.getId()).header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SUP-A"));
    }

    @Test
    void staffCanOpenInactiveSupplierReferencedByOldReceipts() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/" + inactive.getId()).header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void unknownSupplierReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/" + UUID.randomUUID()).header("Authorization", staffToken))
                .andExpect(status().isNotFound());
    }

    private static Supplier newSupplier(String code, String name, boolean active) {
        Supplier s = new Supplier();
        s.setCode(code);
        s.setName(name);
        s.setIsActive(active);
        return s;
    }
}
