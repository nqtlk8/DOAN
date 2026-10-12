package com.storename.erp.crm.api;

import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.common.security.JwtAuthenticationFilter;
import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.domain.ReceivableDebt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API thu nợ độc lập: POST /api/v1/receivable-debts/{customerId}/payments (chỉ có ở server chi nhánh). */
@WebMvcTest(controllers = DebtPaymentController.class, properties = "instance.role=BRANCH")
@org.springframework.context.annotation.Import(com.storename.erp.common.security.SecurityConfig.class)
class DebtPaymentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ReceivableDebtService debtService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UUID customerId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private UsernamePasswordAuthenticationToken auth(String branchId, String authority) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId.toString(), null, List.of(new SimpleGrantedAuthority(authority)));
        auth.setDetails(new JwtAuthDetails(branchId, "token-1"));
        return auth;
    }

    private String url() {
        return "/api/v1/receivable-debts/" + customerId + "/payments";
    }

    @Test
    void recordPayment_returns201_andCallsServiceWithTokenBranch() throws Exception {
        ReceivableDebt debt = new ReceivableDebt();
        debt.setBranchId(1L);
        debt.setTotalDebt(new BigDecimal("300000"));
        when(debtService.recordPayment(eq(customerId), eq(1L), any(), eq(userId), eq("Tiền mặt"))).thenReturn(debt);

        mockMvc.perform(post(url()).with(authentication(auth("1", "STAFF")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 200000, \"note\": \"Tiền mặt\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalDebt").value(300000));

        verify(debtService).recordPayment(eq(customerId), eq(1L),
                argThat(a -> a.compareTo(new BigDecimal("200000")) == 0), eq(userId), eq("Tiền mặt"));
    }

    @Test
    void recordPayment_withoutBranchInToken_is403() throws Exception {
        mockMvc.perform(post(url()).with(authentication(auth("HQ", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1000}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(debtService);
    }

    @Test
    void recordPayment_zeroOrMissingAmount_is400() throws Exception {
        mockMvc.perform(post(url()).with(authentication(auth("1", "STAFF")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(url()).with(authentication(auth("1", "STAFF")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(debtService);
    }

    @Test
    void recordPayment_overDebt_is409WithCode() throws Exception {
        when(debtService.recordPayment(any(), any(), any(), any(), any()))
                .thenThrow(new BusinessConflictException(ReceivableDebtService.DEBT_NEGATIVE, "Công nợ không được âm"));

        mockMvc.perform(post(url()).with(authentication(auth("1", "STAFF")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 999999999}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors[0]").value(ReceivableDebtService.DEBT_NEGATIVE));
    }

    @Test
    void recordPayment_unauthenticated_is403() throws Exception {
        mockMvc.perform(post(url()).contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 1}"))
                .andExpect(status().isForbidden());
    }
}

/** Trên HQ không có API thu nợ: công nợ do chi nhánh sở hữu, HQ chỉ nhận bản đồng bộ. */
@WebMvcTest(controllers = DebtPaymentController.class, properties = "instance.role=HQ")
@org.springframework.context.annotation.Import(com.storename.erp.common.security.SecurityConfig.class)
class DebtPaymentControllerHqTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ReceivableDebtService debtService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void passThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void paymentEndpoint_isNotExposedOnHq() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID().toString(), null, List.of(new SimpleGrantedAuthority("ADMIN")));
        auth.setDetails(new JwtAuthDetails("1", "token-1"));

        mockMvc.perform(post("/api/v1/receivable-debts/" + UUID.randomUUID() + "/payments")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1000}"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(debtService);
    }
}
