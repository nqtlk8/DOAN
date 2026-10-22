package com.storename.erp.branch.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.branch.application.dto.BranchDTO;
import com.storename.erp.branch.domain.Branch;
import com.storename.erp.branch.infrastructure.BranchRepository;
import com.storename.erp.common.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"instance.role=HQ"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
public class BranchRbacIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String globalAdminToken;
    private String branchAdminToken;
    private Long targetBranchId;

    @BeforeEach
    void setUp() {
        branchRepository.deleteAll();

        // Create dummy branch
        Branch branch = Branch.builder()
                .code("BR01")
                .name("Branch 01")
                .isActive(true)
                .build();
        branch = branchRepository.save(branch);
        targetBranchId = branch.getId();

        globalAdminToken = "Bearer " + jwtTokenProvider.generateToken("admin", "ADMIN", null, UUID.randomUUID().toString());
        branchAdminToken = "Bearer " + jwtTokenProvider.generateToken("branch_admin", "ADMIN", targetBranchId.toString(), UUID.randomUUID().toString());
    }

    @AfterEach
    void tearDown() {
        branchRepository.deleteAll();
    }

    @Test
    void globalAdminCanListAllBranches() throws Exception {
        mockMvc.perform(get("/api/v1/branches")
                .header("Authorization", globalAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    void branchAdminCannotListAllBranches() throws Exception {
        mockMvc.perform(get("/api/v1/branches")
                .header("Authorization", branchAdminToken))
                .andExpect(status().isForbidden());
    }
}
