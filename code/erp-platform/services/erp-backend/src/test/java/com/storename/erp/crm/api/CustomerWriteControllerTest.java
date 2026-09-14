package com.storename.erp.crm.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.crm.application.CustomerWriteService;
import com.storename.erp.crm.application.dto.CustomerCreateDto;
import com.storename.erp.crm.application.dto.CustomerResponseDto;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerWriteController.class)
@org.springframework.context.annotation.Import(com.storename.erp.common.security.SecurityConfig.class)
public class CustomerWriteControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @MockBean(name = "auditorProvider")
    private org.springframework.data.domain.AuditorAware<java.util.UUID> auditorProvider;

    @MockBean
    private CustomerWriteService customerWriteService;

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
    void createCustomer_ShouldReturn201_WhenAdmin() throws Exception {
        CustomerCreateDto dto = new CustomerCreateDto();
        dto.setCustomerCode("KH-01");
        dto.setName("New Customer");

        CustomerResponseDto responseDto = new CustomerResponseDto();
        responseDto.setId(UUID.randomUUID());
        responseDto.setCustomerCode("KH-01");
        responseDto.setName("New Customer");

        when(customerWriteService.createCustomer(any(CustomerCreateDto.class))).thenReturn(responseDto);

        JwtAuthDetails details = new JwtAuthDetails("1", "token-123");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ADMIN")));
        auth.setDetails(details);

        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.customerCode").value("KH-01"));
    }

    @Test
    void createCustomer_ShouldReturn403_WhenStaff() throws Exception {
        CustomerCreateDto dto = new CustomerCreateDto();
        dto.setCustomerCode("KH-01");
        dto.setName("New Customer");

        JwtAuthDetails details = new JwtAuthDetails("1", "token-123");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("STAFF")));
        auth.setDetails(details);

        mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isForbidden());
    }
}
