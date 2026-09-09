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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles({"branch", "test"})
@TestPropertySource(properties = {
    "instance.role=BRANCH",
    "branch-id=1001"
})
public class SupplierBranchScopeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String authTokenBranch1001;
    private Supplier supplierBranch1001;
    private Supplier supplierBranch1002;

    @BeforeEach
    void setUp() {
        supplierRepo.deleteAll();

        supplierBranch1001 = new Supplier();
        supplierBranch1001.setCode("SUP-1001");
        supplierBranch1001.setBranchId(1001L);
        supplierBranch1001.setName("Supplier 1001");
        supplierBranch1001 = supplierRepo.save(supplierBranch1001);

        supplierBranch1002 = new Supplier();
        supplierBranch1002.setCode("SUP-1002");
        supplierBranch1002.setBranchId(1002L);
        supplierBranch1002.setName("Supplier 1002");
        supplierBranch1002 = supplierRepo.save(supplierBranch1002);

        authTokenBranch1001 = "Bearer " + jwtTokenProvider.generateToken(UUID.randomUUID().toString(), "STAFF", "1001", UUID.randomUUID().toString());
    }

    @Test
    void staffSeesOnlyOwnBranchSuppliers() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers")
                .header("Authorization", authTokenBranch1001))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Supplier 1001"));
    }

    @Test
    void getSupplierById_wrongBranch_throws403() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/" + supplierBranch1002.getId())
                .header("Authorization", authTokenBranch1001))
                .andExpect(status().isForbidden());
    }
}

