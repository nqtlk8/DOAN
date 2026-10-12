package com.storename.erp.order.api;

import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.common.security.JwtAuthenticationFilter;
import com.storename.erp.order.application.CustomerProductPriceService;
import com.storename.erp.order.application.dto.ResolvedPriceDto;
import com.storename.erp.order.domain.PriceSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authentication dựng giống JwtAuthenticationFilter: principal = username (String),
 * branchId nằm trong details (JwtAuthDetails). Bắt lỗi 500 khi controller đọc branch sai chỗ.
 */
@WebMvcTest(controllers = CustomerProductPriceController.class)
@org.springframework.context.annotation.Import(com.storename.erp.common.security.SecurityConfig.class)
class CustomerProductPriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerProductPriceService priceService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UUID customerId = UUID.fromString("c4c2505e-f208-4f70-b5b2-3cc1c4fb1b81");

    @BeforeEach
    void setUpFilter() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private static UsernamePasswordAuthenticationToken auth(String role, String branchId) {
        UsernamePasswordAuthenticationToken a = new UsernamePasswordAuthenticationToken(
                "staff_tp1", null, List.of(new SimpleGrantedAuthority(role)));
        a.setDetails(new JwtAuthDetails(branchId, "token-1"));
        return a;
    }

    @Test
    void resolve_ShouldUseBranchFromToken_AndReturnPrices() throws Exception {
        when(priceService.resolvePrices(eq(customerId), eq(1L), eq(List.of(1L, 2L)))).thenReturn(List.of(
                new ResolvedPriceDto(1L, new BigDecimal("90000"), PriceSource.CUSTOMER, new BigDecimal("100000"), new BigDecimal("90000")),
                new ResolvedPriceDto(2L, new BigDecimal("50000"), PriceSource.LIST, new BigDecimal("50000"), null)));

        mockMvc.perform(get("/api/v1/customer-prices/{c}/resolve", customerId)
                        .param("productIds", "1,2")
                        .with(authentication(auth("STAFF", "1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productId").value(1))
                .andExpect(jsonPath("$.data[0].source").value("CUSTOMER"))
                .andExpect(jsonPath("$.data[0].unitPrice").value(90000))
                .andExpect(jsonPath("$.data[1].source").value("LIST"));
    }

    @Test
    void resolve_ShouldReturn403_WhenTokenHasNoBranch() throws Exception {
        mockMvc.perform(get("/api/v1/customer-prices/{c}/resolve", customerId)
                        .param("productIds", "1")
                        .with(authentication(auth("ADMIN", "HQ"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(priceService);
    }

    @Test
    void getPrice_ShouldUseBranchFromToken() throws Exception {
        when(priceService.getPrice(customerId, 5L, 1L)).thenReturn(Optional.of(new BigDecimal("12345")));

        mockMvc.perform(get("/api/v1/customer-prices/{c}/product/{p}", customerId, 5L)
                        .with(authentication(auth("STAFF", "1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(12345));
    }
}
