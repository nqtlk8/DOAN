package com.storename.erp.analytics.api;

import com.storename.erp.analytics.api.dto.StockAlertSummaryDto;
import com.storename.erp.analytics.application.StockAlertService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StockAlertController.class)
@org.springframework.context.annotation.Import(com.storename.erp.common.security.SecurityConfig.class)
public class StockAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockAlertService stockAlertService;

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
    void getStockAlerts_ShouldReturn200_WhenAdmin() throws Exception {
        StockAlertSummaryDto dto = StockAlertSummaryDto.builder()
                .negativeCount(1)
                .build();

        when(stockAlertService.getStockAlerts(any())).thenReturn(dto);

        JwtAuthDetails details = new JwtAuthDetails("1", "token-123");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ADMIN")));
        auth.setDetails(details);

        mockMvc.perform(get("/api/v1/analytics/stock-alerts")
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.negativeCount").value(1));
    }

    @Test
    void getStockAlerts_ShouldReturn403_WhenForbidden() throws Exception {
        JwtAuthDetails details = new JwtAuthDetails("1", "token-123");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("STAFF")));
        auth.setDetails(details);

        mockMvc.perform(get("/api/v1/analytics/stock-alerts")
                .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isForbidden());
    }
}
