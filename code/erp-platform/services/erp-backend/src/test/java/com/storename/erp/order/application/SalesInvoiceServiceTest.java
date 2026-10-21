package com.storename.erp.order.application;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceStatus;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SalesInvoiceServiceTest {

    @Mock
    private SalesInvoiceRepository invoiceRepository;
    @Mock
    private ReceivableDebtService debtService;
    @Mock
    private InventoryFacade inventoryFacade;
    @Mock
    private com.storename.erp.crm.api.CrmFacade crmFacade;
    @Mock
    private CustomerProductPriceService customerPriceService;

    @InjectMocks
    private SalesInvoiceService salesInvoiceService;

    private final UUID customerId = UUID.randomUUID();
    private final Long branchId = 1L;

    @BeforeEach
    void setUp() {
    }

    private SalesInvoiceCreateDto dto(String advance) {
        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setCustomerId(customerId);
        dto.setInvoiceCode("HD-TEST");
        if (advance != null) dto.setAdvancePayment(new BigDecimal(advance));
        com.storename.erp.order.application.dto.SalesInvoiceLineDto lineDto = new com.storename.erp.order.application.dto.SalesInvoiceLineDto();
        lineDto.setProductId(1L);
        lineDto.setQuantity(new BigDecimal("2.0"));
        lineDto.setUnitPrice(new BigDecimal("100.0"));
        lineDto.setUnitOfMeasure("Cai");
        dto.setLines(List.of(lineDto));
        return dto;
    }

    /** save() gán id như Hibernate (GenerationType.UUID). */
    private void saveAssignsIds() {
        when(invoiceRepository.save(any(SalesInvoice.class))).thenAnswer(i -> {
            SalesInvoice inv = i.getArgument(0);
            if (inv.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(inv, "id", UUID.randomUUID());
            }
            inv.getLines().forEach(l -> {
                if (l.getId() == null) {
                    org.springframework.test.util.ReflectionTestUtils.setField(l, "id", UUID.randomUUID());
                }
            });
            return inv;
        });
    }

    /** Hóa đơn DRAFT đã lưu (có id) của chi nhánh {@code branchId}. */
    private SalesInvoice draftInvoice() {
        SalesInvoice invoice = new SalesInvoice();
        org.springframework.test.util.ReflectionTestUtils.setField(invoice, "id", UUID.randomUUID());
        invoice.setBranchId(branchId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceCode("HD-DRAFT");
        com.storename.erp.order.domain.SalesInvoiceLine line = new com.storename.erp.order.domain.SalesInvoiceLine();
        org.springframework.test.util.ReflectionTestUtils.setField(line, "id", UUID.randomUUID());
        line.setProductId(1L);
        line.setQuantity(new BigDecimal("2.0"));
        line.setUnitPrice(new BigDecimal("100.0"));
        line.setUnitOfMeasure("Cai");
        line.setLineTotal(new BigDecimal("200.00"));
        invoice.addLine(line);
        invoice.calculateTotal();
        return invoice;
    }

    @Test
    void createDraft_ShouldThrowException_WhenAdvancePaymentNegative() {
        when(crmFacade.customerExists(customerId)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> salesInvoiceService.createDraft(dto("-10.0"), branchId));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createDraft_ShouldThrowException_WhenAdvancePaymentExceedsTotal() {
        when(crmFacade.customerExists(customerId)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> salesInvoiceService.createDraft(dto("250.0"), branchId));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createDraft_ShouldPersistDraft_WithoutInventoryDebtOrPriceEffects() {
        saveAssignsIds();
        when(crmFacade.customerExists(customerId)).thenReturn(true);

        SalesInvoice result = salesInvoiceService.createDraft(dto("50.0"), branchId, "user:key-1");

        assertEquals(SalesInvoiceStatus.DRAFT, result.getStatus());
        assertEquals("user:key-1", result.getRequestKey());
        assertEquals(0, new BigDecimal("200.00").compareTo(result.getTotalAmount()));
        assertEquals(0, new BigDecimal("50.0").compareTo(result.getAdvancePayment()));
        verify(invoiceRepository, times(1)).save(any(SalesInvoice.class));
        verifyNoInteractions(inventoryFacade, debtService, customerPriceService);
    }

    @Test
    void createDraft_SameRequestKey_ShouldReturnExistingInvoice() {
        SalesInvoice existing = draftInvoice();
        when(invoiceRepository.findByRequestKey("user:key-1")).thenReturn(java.util.Optional.of(existing));

        SalesInvoice result = salesInvoiceService.createDraft(dto(null), branchId, "user:key-1");

        assertSame(existing, result);
        verify(invoiceRepository, never()).save(any());
        verifyNoInteractions(crmFacade);
    }

    @Test
    void createDraft_ShouldThrowResourceNotFoundException_WhenCustomerDoesNotExist() {
        when(crmFacade.customerExists(customerId)).thenReturn(false);

        assertThrows(com.storename.erp.common.exception.ResourceNotFoundException.class,
            () -> salesInvoiceService.createDraft(dto(null), branchId));

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void confirmInvoice_ShouldDeductInventory_IncreaseDebt_AndSnapshotDebt() {
        UUID userId = UUID.randomUUID();
        SalesInvoice draft = draftInvoice();
        draft.setAdvancePayment(new BigDecimal("50.0"));
        when(invoiceRepository.findByIdForUpdate(draft.getId())).thenReturn(java.util.Optional.of(draft));
        when(invoiceRepository.save(any(SalesInvoice.class))).thenAnswer(i -> i.getArgument(0));
        when(crmFacade.customerExists(customerId)).thenReturn(true);
        InventoryFacade.SaleCostResult mockResult = new InventoryFacade.SaleCostResult(new BigDecimal("80.0"), "FIFO");
        when(inventoryFacade.recordSaleAndGetCost(eq(1L), eq(branchId), eq(new BigDecimal("2.0")), anyString(), any(), eq(userId)))
                .thenReturn(mockResult);
        // Số dư đọc sau khi đã ghi INVOICE (đã khóa dòng công nợ): 500 nợ cũ + 200 hóa đơn.
        when(debtService.getCurrentDebt(customerId, branchId)).thenReturn(new BigDecimal("700.0"));

        SalesInvoice result = salesInvoiceService.confirmInvoice(draft.getId(), branchId, userId);

        assertEquals(SalesInvoiceStatus.CONFIRMED, result.getStatus());
        assertEquals(userId, result.getConfirmedBy());
        assertEquals(0, new BigDecimal("80.0").compareTo(result.getLines().get(0).getUnitCost()));
        assertEquals("FIFO", result.getLines().get(0).getCostBasis());
        assertEquals(0, new BigDecimal("500.0").compareTo(result.getPreviousDebt()));
        assertEquals(0, new BigDecimal("650.0").compareTo(result.getRemainingDebt()), "500 + 200 - 50 trả trước");

        verify(debtService).increaseDebt(eq(customerId), eq(branchId), eq(new BigDecimal("200.00")), any(), anyString(), anyString(), any(), anyString());
        verify(debtService).decreaseDebt(eq(customerId), eq(branchId), eq(new BigDecimal("50.0")),
                eq(com.storename.erp.crm.domain.ReceivableDebtMovementType.PAYMENT), eq("SALES_INVOICE_ADVANCE"), anyString(), any(), anyString());
        verify(customerPriceService).applyFromInvoice(result, userId);
    }

    @Test
    void confirmInvoice_NotDraft_ShouldBeRejected_WithoutSideEffects() {
        SalesInvoice invoice = draftInvoice();
        invoice.confirm(UUID.randomUUID());
        when(invoiceRepository.findByIdForUpdate(invoice.getId())).thenReturn(java.util.Optional.of(invoice));

        com.storename.erp.common.exception.BusinessConflictException ex = assertThrows(
                com.storename.erp.common.exception.BusinessConflictException.class,
                () -> salesInvoiceService.confirmInvoice(invoice.getId(), branchId, UUID.randomUUID()));

        assertEquals(SalesInvoice.INVALID_STATUS, ex.getCode());
        verifyNoInteractions(inventoryFacade, debtService, customerPriceService);
    }

    @Test
    void confirmInvoice_OtherBranch_ShouldBeForbidden() {
        SalesInvoice invoice = draftInvoice();
        when(invoiceRepository.findByIdForUpdate(invoice.getId())).thenReturn(java.util.Optional.of(invoice));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> salesInvoiceService.confirmInvoice(invoice.getId(), branchId + 1, UUID.randomUUID()));
        verifyNoInteractions(inventoryFacade, debtService, customerPriceService);
    }

    @Test
    void confirmInvoice_NotFound_ShouldThrowResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findByIdForUpdate(id)).thenReturn(java.util.Optional.empty());

        assertThrows(com.storename.erp.common.exception.ResourceNotFoundException.class,
                () -> salesInvoiceService.confirmInvoice(id, branchId, UUID.randomUUID()));
    }

}
