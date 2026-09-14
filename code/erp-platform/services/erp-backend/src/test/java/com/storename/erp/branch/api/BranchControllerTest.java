package com.storename.erp.branch.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.branch.application.BranchService;
import com.storename.erp.branch.application.dto.BranchDTO;
import com.storename.erp.common.security.JwtAuthenticationFilter;
import com.storename.erp.common.security.JwtAuthDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BranchController.class)
@org.springframework.context.annotation.Import(com.storename.erp.common.security.SecurityConfig.class)
public class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BranchService branchService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUpFilter() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void createBranch_ShouldReturn200_WhenAdmin() throws Exception {
        BranchDTO dto = new BranchDTO();
        dto.setCode("BR-01");
        dto.setName("Branch 1");

        when(branchService.createBranch(any(BranchDTO.class))).thenReturn(dto);

        JwtAuthDetails details = new JwtAuthDetails("1", "token-123");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ADMIN")));
        auth.setDetails(details);

        mockMvc.perform(post("/api/v1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("BR-01"));
    }

    @Test
    void createBranch_ShouldReturn403_WhenForbidden() throws Exception {
        BranchDTO dto = new BranchDTO();
        dto.setCode("BR-01");
        dto.setName("Branch 1");

        JwtAuthDetails details = new JwtAuthDetails("1", "token-123");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("STAFF")));
        auth.setDetails(details);

        mockMvc.perform(post("/api/v1/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isForbidden());
    }
}

