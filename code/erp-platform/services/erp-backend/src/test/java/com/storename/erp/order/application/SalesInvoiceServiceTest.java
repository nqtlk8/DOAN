package com.storename.erp.order.application;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private SalesInvoiceService salesInvoiceService;

    private final UUID customerId = UUID.randomUUID();
    private final Long branchId = 1L;

    @BeforeEach
    void setUp() {
    }

    @Test
    void createDraft_ShouldSaveAndReturnInvoice() {
        SalesInvoiceCreateDto dto = new SalesInvoiceCreateDto();
        dto.setCustomerId(customerId);
        dto.setInvoiceCode("HD-TEST");
        dto.setPaymentMethod(com.storename.erp.order.domain.PaymentMethod.CASH);
        
        com.storename.erp.order.application.dto.SalesInvoiceLineDto lineDto = new com.storename.erp.order.application.dto.SalesInvoiceLineDto();
        lineDto.setProductId(1L);
        lineDto.setQuantity(new BigDecimal("2.0"));
        lineDto.setUnitPrice(new BigDecimal("100.0"));
        lineDto.setUnitOfMeasure("Cai");
        dto.setLines(List.of(lineDto));

        when(invoiceRepository.save(any(SalesInvoice.class))).thenAnswer(i -> {
            SalesInvoice inv = i.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(inv, "id", UUID.randomUUID());
            return inv;
        });

        SalesInvoice invoice = salesInvoiceService.createDraft(dto, branchId);

        assertNotNull(invoice);
        assertEquals(SalesInvoiceStatus.DRAFT, invoice.getStatus());
        assertEquals("HD-TEST", invoice.getInvoiceCode());
        assertEquals(0, new BigDecimal("200.0").compareTo(invoice.getTotalAmount()));
        verify(invoiceRepository).save(any(SalesInvoice.class));
    }

    @Test
    void confirmInvoice_ShouldDeductInventoryAndIncreaseDebt() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        SalesInvoice invoice = new SalesInvoice();
        org.springframework.test.util.ReflectionTestUtils.setField(invoice, "id", invoiceId);
        invoice.setBranchId(branchId);
        invoice.setCustomerId(customerId);
        invoice.setStatus(SalesInvoiceStatus.DRAFT);
        
        SalesInvoiceLine line = new SalesInvoiceLine();
        org.springframework.test.util.ReflectionTestUtils.setField(line, "id", UUID.randomUUID());
        line.setProductId(1L);
        line.setQuantity(new BigDecimal("2.0"));
        line.setUnitPrice(new BigDecimal("100.0"));
        line.setLineTotal(new BigDecimal("200.0"));
        invoice.addLine(line);
        invoice.calculateTotal();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        
        InventoryFacade.SaleCostResult mockResult = new InventoryFacade.SaleCostResult(new BigDecimal("80.0"), "FIFO");
        when(inventoryFacade.recordSaleAndGetCost(eq(1L), eq(branchId), eq(new BigDecimal("2.0")), eq(invoiceId.toString()), eq(line.getId()), isNull()))
                .thenReturn(mockResult);
                
        when(debtService.getCurrentDebt(customerId, branchId)).thenReturn(new BigDecimal("500.0"));
        when(invoiceRepository.save(any(SalesInvoice.class))).thenAnswer(i -> i.getArgument(0));

        SalesInvoice confirmed = salesInvoiceService.confirmInvoice(invoiceId, userId, branchId);

        assertEquals(SalesInvoiceStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(0, new BigDecimal("80.0").compareTo(confirmed.getLines().get(0).getUnitCost()));
        assertEquals("FIFO", confirmed.getLines().get(0).getCostBasis());
        
        verify(debtService).increaseDebt(customerId, branchId, new BigDecimal("200.0"));
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void confirmInvoice_ShouldThrowException_WhenNotDraft() {
        UUID invoiceId = UUID.randomUUID();
        SalesInvoice invoice = new SalesInvoice();
        invoice.setBranchId(branchId);
        invoice.setStatus(SalesInvoiceStatus.CONFIRMED);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        assertThrows(IllegalStateException.class, () -> salesInvoiceService.confirmInvoice(invoiceId, UUID.randomUUID(), branchId));
    }
}
