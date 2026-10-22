package com.storename.erp.crm.api;

import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.common.security.JwtAuthenticationFilter;
import com.storename.erp.crm.application.CashReceiptService;
import com.storename.erp.crm.application.dto.CashReceiptResponseDto;
import com.storename.erp.crm.domain.CashReceipt;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CashReceiptController.class)
// Profile mặc định là hq (application.yml) -> instance.role=HQ tắt controller giao dịch -> 404. Chạy như chi nhánh.
@org.springframework.test.context.TestPropertySource(properties = "instance.role=BRANCH")
@org.springframework.context.annotation.Import(com.storename.erp.common.security.SecurityConfig.class)
class CashReceiptControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CashReceiptService cashReceiptService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UUID userId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUpFilter() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private UsernamePasswordAuthenticationToken auth(String role, String branchId) {
        UsernamePasswordAuthenticationToken a = new UsernamePasswordAuthenticationToken(
                userId.toString(), null, List.of(new SimpleGrantedAuthority(role)));
        a.setDetails(new JwtAuthDetails(branchId, "token-1"));
        return a;
    }

    private String body(String amount) {
        return "{\"customerId\":\"" + customerId + "\",\"amount\":" + amount + ",\"receiptDate\":\"2026-10-18\",\"note\":\"Trả nợ\"}";
    }

    @Test
    void U_PT_C01_staffLapPhieu_201() throws Exception {
        CashReceipt r = new CashReceipt();
        r.setId(UUID.randomUUID());
        r.setReceiptCode("PT12345678");
        when(cashReceiptService.create(any(), eq(1L), eq(userId), any())).thenReturn(r);

        mockMvc.perform(post("/api/v1/cash-receipts").with(authentication(auth("STAFF", "1")))
                        .contentType(MediaType.APPLICATION_JSON).content(body("4000000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.receiptCode").value("PT12345678"));
    }

    @Test
    void U_PT_C02_adminKhongLapDuocPhieu_403() throws Exception {
        mockMvc.perform(post("/api/v1/cash-receipts").with(authentication(auth("ADMIN", "HQ")))
                        .contentType(MediaType.APPLICATION_JSON).content(body("4000000")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cashReceiptService);
    }

    @Test
    void U_PT_C03_chuaDangNhap_403() throws Exception {
        mockMvc.perform(post("/api/v1/cash-receipts").contentType(MediaType.APPLICATION_JSON).content(body("1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void U_PT_C04_soTienBang0_400() throws Exception {
        mockMvc.perform(post("/api/v1/cash-receipts").with(authentication(auth("STAFF", "1")))
                        .contentType(MediaType.APPLICATION_JSON).content(body("0")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cashReceiptService);
    }

    @Test
    void U_PT_C05_thieuKhachHang_400() throws Exception {
        mockMvc.perform(post("/api/v1/cash-receipts").with(authentication(auth("STAFF", "1")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void U_PT_C06_huyPhieu_200_traVeTrangThaiDaHuy() throws Exception {
        UUID id = UUID.randomUUID();
        when(cashReceiptService.get(id, 1L)).thenReturn(CashReceiptResponseDto.builder()
                .id(id).receiptCode("PT12345678").status("CANCELLED").amount(new BigDecimal("1000")).build());

        mockMvc.perform(post("/api/v1/cash-receipts/" + id + "/cancel").with(authentication(auth("STAFF", "1")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Nhập sai\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        verify(cashReceiptService).cancel(id, "Nhập sai", 1L, userId);
    }

    @Test
    void U_PT_C07_danhSachTheoChiNhanhTrongToken() throws Exception {
        when(cashReceiptService.list(1L)).thenReturn(List.of(CashReceiptResponseDto.builder().receiptCode("PT1").build()));

        mockMvc.perform(get("/api/v1/cash-receipts").with(authentication(auth("STAFF", "1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].receiptCode").value("PT1"));
    }
}
