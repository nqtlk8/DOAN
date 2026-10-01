package com.storename.erp.catalog.api;

import com.storename.erp.catalog.application.CategoryReader;
import com.storename.erp.catalog.application.dto.CategoryResponseDto;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.common.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryReadController.class)
@org.springframework.context.annotation.Import(com.storename.erp.common.security.SecurityConfig.class)
class CategoryReadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryReader categoryReader;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void staffGetsCategoryTreeWithParentInfo() throws Exception {
        when(categoryReader.getActiveCategories()).thenReturn(List.of(
                CategoryResponseDto.builder().id(1L).code("vlxd").name("VẬT LIỆU XÂY DỰNG").isActive(true).build(),
                CategoryResponseDto.builder().id(3L).code("chau-rua").name("Chậu Rửa - Lavabo")
                        .parentId(1L).parentName("VẬT LIỆU XÂY DỰNG").isActive(true).build()));

        UsernamePasswordAuthenticationToken staff = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("STAFF")));
        staff.setDetails(new JwtAuthDetails("1", "token-1"));

        mockMvc.perform(get("/api/v1/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].parentId").doesNotExist())
                .andExpect(jsonPath("$.data[1].code").value("chau-rua"))
                .andExpect(jsonPath("$.data[1].parentId").value(1))
                .andExpect(jsonPath("$.data[1].parentName").value("VẬT LIỆU XÂY DỰNG"))
                .andExpect(jsonPath("$.data[1].isActive").value(true));
    }

    @Test
    void unauthenticatedIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/categories"))
                .andExpect(status().isForbidden());
    }
}
