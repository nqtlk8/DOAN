package com.storename.erp.inventory.api;

import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import com.storename.erp.common.security.JwtAuthenticationFilter;
import com.storename.erp.common.security.JwtAuthDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StockController.class)
@org.springframework.context.annotation.Import(com.storename.erp.common.security.SecurityConfig.class)
public class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.storename.erp.inventory.application.StockService stockService;

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
    void getStock_ShouldReturn200_WhenAuthorized() throws Exception {
        com.storename.erp.inventory.application.dto.StockOnHandResponseDto dto = com.storename.erp.inventory.application.dto.StockOnHandResponseDto.builder()
                .productId(10L)
                .branchId(1L)
                .quantity(new BigDecimal("100"))
                .build();

        when(stockService.getStockByBranch(1L)).thenReturn(List.of(dto));

        JwtAuthDetails details = new JwtAuthDetails("1", "token-123");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("STAFF")));
        auth.setDetails(details);

        mockMvc.perform(get("/api/v1/inventory/stock")
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].quantity").value(100.0));
    }

    @Test
    void getStock_ShouldReturn403_WhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/stock"))
                .andExpect(status().isForbidden());
    }
}

