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
public class BranchRbacIntegrationTest {

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
        mockMvc.perform(get("/api/branches")
                .header("Authorization", globalAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    void branchAdminCannotListAllBranches() throws Exception {
        mockMvc.perform(get("/api/branches")
                .header("Authorization", branchAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void branchAdminCanGetOwnBranch() throws Exception {
        mockMvc.perform(get("/api/branches/" + targetBranchId)
                .header("Authorization", branchAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    void branchAdminCannotGetOtherBranch() throws Exception {
        Branch otherBranch = Branch.builder()
                .code("BR02")
                .name("Branch 02")
                .isActive(true)
                .build();
        otherBranch = branchRepository.save(otherBranch);

        mockMvc.perform(get("/api/branches/" + otherBranch.getId())
                .header("Authorization", branchAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void branchAdminCannotCreateBranch() throws Exception {
        BranchDTO dto = BranchDTO.builder()
                .code("BR03")
                .name("Branch 03")
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/branches")
                .header("Authorization", branchAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void globalAdminCanCreateBranch() throws Exception {
        BranchDTO dto = BranchDTO.builder()
                .code("BR04")
                .name("Branch 04")
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/branches")
                .header("Authorization", globalAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}
