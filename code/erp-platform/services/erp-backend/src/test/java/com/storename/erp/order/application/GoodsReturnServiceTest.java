package com.storename.erp.order.application;

import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.order.application.dto.GoodsReturnCreateDto;
import com.storename.erp.order.domain.GoodsReturn;
import com.storename.erp.order.domain.GoodsReturnLine;
import com.storename.erp.order.domain.GoodsReturnStatus;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.domain.SalesInvoiceStatus;
import com.storename.erp.order.infrastructure.GoodsReturnRepository;
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
public class GoodsReturnServiceTest {

    @Mock
    private GoodsReturnRepository returnRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ReceivableDebtService debtService;
    @Mock
    private SalesInvoiceRepository invoiceRepository;
    @Mock
    private InventoryFacade inventoryFacade;

    @InjectMocks
    private GoodsReturnService goodsReturnService;

    private Customer mockCustomer;
    private final UUID customerId = UUID.randomUUID();
    private final Long branchId = 1L;

    @BeforeEach
    void setUp() {
        mockCustomer = new Customer();
        mockCustomer.setId(customerId);
    }

    @Test
    void createDraft_ShouldSaveAndReturnId() {
        GoodsReturnCreateDto dto = new GoodsReturnCreateDto();
        dto.setCustomerId(customerId);
        GoodsReturnCreateDto.LineDto line = new GoodsReturnCreateDto.LineDto();
        line.setProductId(10L);
        line.setQuantity(new BigDecimal("2.0"));
        line.setUnitPrice(new BigDecimal("50.0"));
        line.setUnitOfMeasure("Cai");
        dto.setLines(List.of(line));

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(returnRepository.save(any(GoodsReturn.class))).thenAnswer(i -> {
            GoodsReturn gr = i.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(gr, "id", UUID.randomUUID());
            return gr;
        });

        UUID id = goodsReturnService.createDraft(branchId, dto);

        assertNotNull(id);
        verify(returnRepository).save(argThat(gr -> 
            gr.getStatus() == GoodsReturnStatus.DRAFT &&
            gr.getTotalAmount().compareTo(new BigDecimal("100.0")) == 0
        ));
    }

    @Test
    void confirmReturn_ShouldUpdateInventoryAndDebt() {
        UUID returnId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        GoodsReturn goodsReturn = new GoodsReturn();
        org.springframework.test.util.ReflectionTestUtils.setField(goodsReturn, "id", returnId);
        goodsReturn.setBranchId(branchId);
        goodsReturn.setCustomer(mockCustomer);
        goodsReturn.setInvoiceId(invoiceId);
        
        GoodsReturnLine line = new GoodsReturnLine();
        org.springframework.test.util.ReflectionTestUtils.setField(line, "id", UUID.randomUUID());
        line.setProductId(10L);
        line.setQuantity(new BigDecimal("2.0"));
        line.setUnitPrice(new BigDecimal("50.0"));
        line.setUnitOfMeasure("Cai");
        goodsReturn.addLine(line);
        goodsReturn.calculateTotal();

        SalesInvoice invoice = new SalesInvoice();
        org.springframework.test.util.ReflectionTestUtils.setField(invoice, "id", invoiceId);
        invoice.setBranchId(branchId);
        invoice.setCustomerId(customerId);
        invoice.setStatus(SalesInvoiceStatus.CONFIRMED);
        
        SalesInvoiceLine invoiceLine = new SalesInvoiceLine();
        invoiceLine.setProductId(10L);
        invoiceLine.setUnitOfMeasure("Cai");
        invoiceLine.setQuantity(new BigDecimal("5.0"));
        invoice.addLine(invoiceLine);

        when(returnRepository.findById(returnId)).thenReturn(Optional.of(goodsReturn));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(returnRepository.getTotalReturnedQuantity(invoiceId, 10L)).thenReturn(BigDecimal.ZERO);

        goodsReturnService.confirmReturn(returnId, branchId, userId);

        assertEquals(GoodsReturnStatus.CONFIRMED, goodsReturn.getStatus());
        verify(inventoryFacade).recordReturn(eq(10L), eq(branchId), eq(new BigDecimal("2.0")), eq(new BigDecimal("50.0")), eq(returnId.toString()), eq(line.getId()), eq(userId));
        verify(debtService).decreaseDebt(eq(customerId), eq(branchId), argThat(a -> a.compareTo(new BigDecimal("100.0")) == 0), any(), anyString(), anyString(), any(), anyString());
        verify(returnRepository).save(goodsReturn);
    }
}
