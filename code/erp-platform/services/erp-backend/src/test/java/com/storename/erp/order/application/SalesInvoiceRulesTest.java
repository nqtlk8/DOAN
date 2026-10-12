package com.storename.erp.order.application;

import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.crm.api.CrmFacade;
import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceLineDto;
import com.storename.erp.order.domain.PaymentMethod;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceStatus;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Thứ tự khóa dòng tồn, yêu cầu chi nhánh và mã lỗi của SalesInvoiceService. */
@ExtendWith(MockitoExtension.class)
class SalesInvoiceRulesTest {

    @Mock private SalesInvoiceRepository invoiceRepository;
    @Mock private ReceivableDebtService debtService;
    @Mock private InventoryFacade inventoryFacade;
    @Mock private CrmFacade crmFacade;
    @Mock private CustomerProductPriceService customerPriceService;

    @InjectMocks private SalesInvoiceService service;

    private final Long branchId = 1L;
    private final UUID customerId = UUID.randomUUID();

    private SalesInvoiceCreateDto dtoWithProducts(long... productIds) {
        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setCustomerId(customerId);
        dto.setPaymentMethod(PaymentMethod.CREDIT);
        List<SalesInvoiceLineDto> lines = new ArrayList<>();
        for (long pid : productIds) {
            SalesInvoiceLineDto l = new SalesInvoiceLineDto();
            l.setProductId(pid);
            l.setProductName("SP " + pid);
            l.setQuantity(BigDecimal.ONE);
            l.setUnitPrice(new BigDecimal("100"));
            l.setUnitOfMeasure("Cai");
            lines.add(l);
        }
        dto.setLines(lines);
        return dto;
    }

    @Test
    @DisplayName("Xuất kho theo productId tăng dần, bất kể thứ tự dòng trên hóa đơn (chống deadlock)")
    void createAndConfirm_locksStockInProductIdOrder() {
        when(crmFacade.customerExists(customerId)).thenReturn(true);
        when(invoiceRepository.save(any(SalesInvoice.class))).thenAnswer(i -> {
            SalesInvoice inv = i.getArgument(0);
            if (inv.getId() == null) {
                ReflectionTestUtils.setField(inv, "id", UUID.randomUUID());
                inv.getLines().forEach(l -> ReflectionTestUtils.setField(l, "id", UUID.randomUUID()));
            }
            return inv;
        });
        when(inventoryFacade.recordSaleAndGetCost(any(), any(), any(), anyString(), any(), any()))
                .thenReturn(new InventoryFacade.SaleCostResult(new BigDecimal("50"), "NORMAL"));
        when(debtService.getCurrentDebt(customerId, branchId)).thenReturn(BigDecimal.ZERO);

        SalesInvoice invoice = service.createAndConfirm(dtoWithProducts(30L, 10L, 20L), branchId, UUID.randomUUID());

        InOrder order = inOrder(inventoryFacade);
        order.verify(inventoryFacade).recordSaleAndGetCost(eq(10L), any(), any(), anyString(), any(), any());
        order.verify(inventoryFacade).recordSaleAndGetCost(eq(20L), any(), any(), anyString(), any(), any());
        order.verify(inventoryFacade).recordSaleAndGetCost(eq(30L), any(), any(), anyString(), any(), any());
        // Thứ tự dòng trên hóa đơn giữ nguyên như người dùng nhập.
        assertThat(invoice.getLines()).extracting("productId").containsExactly(30L, 10L, 20L);
        assertThat(invoice.getStatus()).isEqualTo(SalesInvoiceStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Token không có chi nhánh -> 403, không ghi gì")
    void createAndConfirm_withoutBranch_isForbidden() {
        assertThatThrownBy(() -> service.createAndConfirm(dtoWithProducts(1L), null, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(invoiceRepository, inventoryFacade, debtService);
    }

    @Test
    @DisplayName("Xác nhận hóa đơn không tồn tại -> 404 (trước đây RuntimeException -> 500)")
    void confirmInvoice_notFound_is404() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmInvoice(id, UUID.randomUUID(), branchId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Xác nhận hóa đơn đã CONFIRMED -> 409 INVALID_STATUS")
    void confirmInvoice_alreadyConfirmed_is409() {
        UUID id = UUID.randomUUID();
        SalesInvoice invoice = new SalesInvoice();
        invoice.setBranchId(branchId);
        invoice.setInvoiceCode("HD-1");
        invoice.setStatus(SalesInvoiceStatus.CONFIRMED);
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.confirmInvoice(id, UUID.randomUUID(), branchId))
                .isInstanceOf(BusinessConflictException.class)
                .extracting("code").isEqualTo(SalesInvoice.INVALID_STATUS);
    }

    @Test
    @DisplayName("Xem hóa đơn chi nhánh khác -> 403 (trước đây RuntimeException -> 500); ADMIN HQ xem được")
    void getInvoice_branchRules() {
        UUID id = UUID.randomUUID();
        SalesInvoice invoice = new SalesInvoice();
        invoice.setBranchId(2L);
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.getInvoice(id, branchId)).isInstanceOf(AccessDeniedException.class);
        assertThat(service.getInvoice(id, null)).isSameAs(invoice);
    }

    @Test
    @DisplayName("ADMIN HQ (branchId null) xem danh sách hóa đơn mọi chi nhánh")
    void listWithoutBranch_returnsAll() {
        when(invoiceRepository.findAll()).thenReturn(List.of(new SalesInvoice(), new SalesInvoice()));

        assertThat(service.getInvoicesByBranch(null)).hasSize(2);
        verify(invoiceRepository, never()).findByBranchId(any());
    }
}
