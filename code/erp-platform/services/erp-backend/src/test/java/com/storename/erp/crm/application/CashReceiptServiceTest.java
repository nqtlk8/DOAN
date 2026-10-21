package com.storename.erp.crm.application;

import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.crm.application.dto.CashReceiptCreateDto;
import com.storename.erp.crm.domain.CashReceipt;
import com.storename.erp.crm.domain.CashReceiptStatus;
import com.storename.erp.crm.domain.ReceivableDebt;
import com.storename.erp.crm.domain.ReceivableDebtMovementType;
import com.storename.erp.crm.infrastructure.CashReceiptRepository;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phiếu thu: lập phiếu giảm công nợ (PAYMENT / CASH_RECEIPT), hủy phiếu tăng lại công nợ
 * (ADJUSTMENT / CASH_RECEIPT_CANCEL).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CashReceiptServiceTest {

    private static final String ZONE = "Asia/Ho_Chi_Minh";

    @Mock private CashReceiptRepository receiptRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ReceivableDebtService debtService;

    private CashReceiptService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final Long branchId = 1L;

    @BeforeEach
    void setUp() {
        service = new CashReceiptService(receiptRepository, customerRepository, debtService, ZONE);
        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(receiptRepository.existsByReceiptCode(anyString())).thenReturn(false);
        when(receiptRepository.save(any(CashReceipt.class))).thenAnswer(inv -> {
            CashReceipt r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
    }

    private CashReceiptCreateDto dto(String amount) {
        CashReceiptCreateDto dto = new CashReceiptCreateDto();
        dto.setCustomerId(customerId);
        dto.setAmount(new BigDecimal(amount));
        dto.setReceiptDate(LocalDate.now(ZoneId.of(ZONE)));
        dto.setNote("Trả nợ đơn HD001");
        return dto;
    }

    private static ReceivableDebt debtWithBalance(String balance) {
        ReceivableDebt d = new ReceivableDebt();
        d.setTotalDebt(new BigDecimal(balance));
        return d;
    }

    private void givenDebt(String current, String afterDecrease) {
        when(debtService.getCurrentDebt(customerId, branchId)).thenReturn(new BigDecimal(current));
        when(debtService.decreaseDebt(eq(customerId), eq(branchId), any(), any(), any(), any(), any(), any()))
                .thenReturn(debtWithBalance(afterDecrease));
    }

    @Test
    void U_PT_01_thuMotPhan_giamNoVaLuuSnapshot() {
        givenDebt("4000000", "3000000");

        CashReceipt r = service.create(dto("1000000"), branchId, userId);

        assertNotNull(r.getId());
        assertTrue(r.getReceiptCode().matches("PT[0-9A-F]{8}"), r.getReceiptCode());
        assertEquals(CashReceiptStatus.POSTED, r.getStatus());
        assertEquals(0, r.getDebtBefore().compareTo(new BigDecimal("4000000")));
        assertEquals(0, r.getDebtAfter().compareTo(new BigDecimal("3000000")));
        assertEquals(branchId, r.getBranchId());
        verify(debtService).decreaseDebt(eq(customerId), eq(branchId), eq(new BigDecimal("1000000")),
                eq(ReceivableDebtMovementType.PAYMENT), eq(CashReceiptService.REF_RECEIPT),
                eq(r.getId().toString()), eq(userId), contains(r.getReceiptCode()));
    }

    @Test
    void U_PT_02_thuHet_noVeKhong() {
        givenDebt("4000000", "0");

        CashReceipt r = service.create(dto("4000000"), branchId, userId);

        assertEquals(0, r.getDebtAfter().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.getDebtBefore().compareTo(new BigDecimal("4000000")));
    }

    @Test
    void U_PT_03_thuVuotNo_bi409VaKhongLuuPhieu() {
        givenDebt("4000000", "0");

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> service.create(dto("4000001"), branchId, userId));

        assertEquals(ReceivableDebtService.DEBT_NEGATIVE, ex.getCode());
        verify(receiptRepository, never()).save(any());
        verify(debtService, never()).decreaseDebt(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void U_PT_04_khachKhongConNo_bi409() {
        when(debtService.getCurrentDebt(customerId, branchId)).thenReturn(BigDecimal.ZERO);

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> service.create(dto("1000"), branchId, userId));

        assertEquals(CashReceiptService.NO_DEBT, ex.getCode());
        verify(receiptRepository, never()).save(any());
    }

    @Test
    void U_PT_05_tokenKhongCoChiNhanh_bi403() {
        assertThrows(AccessDeniedException.class, () -> service.create(dto("1000"), null, userId));
        verifyNoInteractions(debtService);
    }

    @Test
    void U_PT_06_ngayThuTuongLai_bi400() {
        givenDebt("4000000", "3000000");
        CashReceiptCreateDto dto = dto("1000000");
        dto.setReceiptDate(LocalDate.now(ZoneId.of(ZONE)).plusDays(1));

        assertThrows(IllegalArgumentException.class, () -> service.create(dto, branchId, userId));
        verify(receiptRepository, never()).save(any());
    }

    @Test
    void U_PT_07_khongNhapNgay_macDinhHomNay_choPhepNgayQuaKhu() {
        givenDebt("4000000", "3000000");
        CashReceiptCreateDto noDate = dto("1000000");
        noDate.setReceiptDate(null);
        assertEquals(LocalDate.now(ZoneId.of(ZONE)), service.create(noDate, branchId, userId).getReceiptDate());

        CashReceiptCreateDto past = dto("1000000");
        past.setReceiptDate(LocalDate.now(ZoneId.of(ZONE)).minusDays(3));
        assertEquals(past.getReceiptDate(), service.create(past, branchId, userId).getReceiptDate());
    }

    @Test
    void U_PT_08_soTienKhongDuong_bi400() {
        assertThrows(IllegalArgumentException.class, () -> service.create(dto("0"), branchId, userId));
        assertThrows(IllegalArgumentException.class, () -> service.create(dto("-5"), branchId, userId));
        verifyNoInteractions(debtService);
    }

    @Test
    void U_PT_09_khachChuaDongBo_bi404() {
        when(customerRepository.existsById(customerId)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.create(dto("1000"), branchId, userId));
        verifyNoInteractions(debtService);
    }

    @Test
    void U_PT_10_snapshotTheoSoDuDaKhoa_khongTheoSoDocTruoc() {
        // Đọc trước thấy nợ 4tr, nhưng lúc khóa dòng công nợ người khác vừa thu 1tr -> sau khi thu còn 2tr.
        givenDebt("4000000", "2000000");

        CashReceipt r = service.create(dto("1000000"), branchId, userId);

        assertEquals(0, r.getDebtBefore().compareTo(new BigDecimal("3000000")));
        assertEquals(0, r.getDebtAfter().compareTo(new BigDecimal("2000000")));
        assertEquals(0, r.getDebtBefore().subtract(r.getAmount()).compareTo(r.getDebtAfter()));
    }

    @Test
    void U_PT_11_ghiChuTrong_luuNull_vaNoteCuaSoCongNoChiCoMaPhieu() {
        givenDebt("4000000", "3000000");
        CashReceiptCreateDto dto = dto("1000000");
        dto.setNote("   ");

        CashReceipt r = service.create(dto, branchId, userId);

        assertNull(r.getNote());
        ArgumentCaptor<String> note = ArgumentCaptor.forClass(String.class);
        verify(debtService).decreaseDebt(any(), any(), any(), any(), any(), any(), any(), note.capture());
        assertEquals("Phiếu thu " + r.getReceiptCode(), note.getValue());
    }

    private CashReceipt postedReceipt(Long ownerBranch) {
        CashReceipt r = new CashReceipt();
        r.setId(UUID.randomUUID());
        r.setReceiptCode("PTAAAA0001");
        r.setBranchId(ownerBranch);
        r.setCustomerId(customerId);
        r.setReceiptDate(LocalDate.now());
        r.setAmount(new BigDecimal("1000000"));
        r.snapshotDebt(new BigDecimal("4000000"), new BigDecimal("3000000"));
        return r;
    }

    @Test
    void U_PT_12_huyPhieu_tangLaiCongNoVaDanhDauDaHuy() {
        CashReceipt r = postedReceipt(branchId);
        when(receiptRepository.findForUpdate(r.getId())).thenReturn(Optional.of(r));

        CashReceipt cancelled = service.cancel(r.getId(), "  Nhập sai số tiền ", branchId, userId);

        assertEquals(CashReceiptStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Nhập sai số tiền", cancelled.getCancelReason());
        assertEquals(userId, cancelled.getCancelledBy());
        assertNotNull(cancelled.getCancelledAt());
        verify(debtService).increaseDebt(eq(customerId), eq(branchId), eq(new BigDecimal("1000000")),
                eq(ReceivableDebtMovementType.ADJUSTMENT), eq(CashReceiptService.REF_RECEIPT_CANCEL),
                eq(r.getId().toString()), eq(userId), contains("Hủy phiếu thu PTAAAA0001"));
    }

    @Test
    void U_PT_13_huyPhieuDaHuy_bi409VaKhongCongNoLan2() {
        CashReceipt r = postedReceipt(branchId);
        r.cancel(userId, null);
        when(receiptRepository.findForUpdate(r.getId())).thenReturn(Optional.of(r));

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> service.cancel(r.getId(), null, branchId, userId));

        assertEquals(CashReceipt.ALREADY_CANCELLED, ex.getCode());
        verify(debtService, never()).increaseDebt(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void U_PT_14_huyPhieuChiNhanhKhac_bi403() {
        CashReceipt r = postedReceipt(2L);
        when(receiptRepository.findForUpdate(r.getId())).thenReturn(Optional.of(r));

        assertThrows(AccessDeniedException.class, () -> service.cancel(r.getId(), null, branchId, userId));
        assertEquals(CashReceiptStatus.POSTED, r.getStatus());
        verifyNoInteractions(debtService);
    }

    @Test
    void U_PT_15_huyPhieuKhongTonTai_bi404() {
        UUID id = UUID.randomUUID();
        when(receiptRepository.findForUpdate(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.cancel(id, null, branchId, userId));
    }

    @Test
    void U_PT_16_xemPhieuChiNhanhKhac_bi403_admin_xemDuoc() {
        CashReceipt r = postedReceipt(2L);
        when(receiptRepository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThrows(AccessDeniedException.class, () -> service.get(r.getId(), branchId));
        assertEquals("PTAAAA0001", service.get(r.getId(), null).getReceiptCode());
    }

    @Test
    void U_PT_17_maPhieuChoSaoKe() {
        CashReceipt r = postedReceipt(branchId);
        when(receiptRepository.findAllById(any())).thenReturn(List.of(r));

        Map<UUID, String> codes = service.getReceiptCodes(List.of(r.getId()));

        assertEquals("PTAAAA0001", codes.get(r.getId()));
        assertTrue(service.getReceiptCodes(List.of()).isEmpty());
    }
}
