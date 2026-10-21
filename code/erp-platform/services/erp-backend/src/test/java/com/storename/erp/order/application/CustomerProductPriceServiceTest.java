package com.storename.erp.order.application;

import com.storename.erp.catalog.api.CatalogFacade;
import com.storename.erp.order.application.CustomerProductPriceService;
import com.storename.erp.order.application.dto.ResolvedPriceDto;
import com.storename.erp.order.domain.CustomerProductPrice;
import com.storename.erp.order.domain.CustomerProductPriceHistory;
import com.storename.erp.order.domain.PriceSource;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.infrastructure.CustomerProductPriceHistoryRepository;
import com.storename.erp.order.infrastructure.CustomerProductPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerProductPriceServiceTest {

    @Mock
    private CustomerProductPriceRepository priceRepo;
    @Mock
    private CustomerProductPriceHistoryRepository historyRepo;
    @Mock
    private CatalogFacade catalogFacade;

    private CustomerProductPriceService priceService;

    private final UUID customerId = UUID.randomUUID();
    private final Long branchId = 7L;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        priceService = new CustomerProductPriceService(priceRepo, historyRepo, catalogFacade);
    }

    // ------------------------------------------------------------------ getPrice

    // ------------------------------------------------------------------ resolvePrices

    @Test
    void resolvePrices_CustomerPriceWinsOverList_ThenListThenNone_KeepsOrder() {
        CustomerProductPrice expired = customerPrice(3L, "55");
        expired.setEffectiveTo(LocalDateTime.now().minusHours(1));
        when(catalogFacade.getListPrices(eq(branchId), anyCollection()))
                .thenReturn(Map.of(1L, new BigDecimal("100"), 2L, new BigDecimal("200"), 3L, new BigDecimal("300")));
        when(priceRepo.findByCustomerIdAndBranchIdAndProductIdIn(eq(customerId), eq(branchId), anyCollection()))
                .thenReturn(List.of(customerPrice(1L, "90"), expired));

        List<ResolvedPriceDto> result = priceService.resolvePrices(customerId, branchId, List.of(1L, 2L, 3L, 4L, 1L));

        assertEquals(4, result.size(), "trùng productId phải bị bỏ");
        assertResolved(result.get(0), 1L, "90", PriceSource.CUSTOMER, "100");
        assertResolved(result.get(1), 2L, "200", PriceSource.LIST, "200");
        assertResolved(result.get(2), 3L, "300", PriceSource.LIST, "300"); // giá khách hết hạn -> niêm yết
        assertEquals(PriceSource.NONE, result.get(3).source());
        assertNull(result.get(3).unitPrice());
    }

    @Test
    void resolvePrices_WithoutCustomer_UsesListOnly() {
        when(catalogFacade.getListPrices(eq(branchId), anyCollection())).thenReturn(Map.of(1L, new BigDecimal("100")));

        List<ResolvedPriceDto> result = priceService.resolvePrices(null, branchId, List.of(1L));

        assertResolved(result.get(0), 1L, "100", PriceSource.LIST, "100");
        verifyNoInteractions(priceRepo);
    }

    // ------------------------------------------------------------------ applyFromInvoice

    @Test
    void applyFromInvoice_CreatesCustomerPrice_WhenPriceDiffersFromList() {
        SalesInvoice invoice = invoice(line(1L, "95"));
        when(catalogFacade.getListPrices(eq(branchId), anyCollection())).thenReturn(Map.of(1L, new BigDecimal("100")));
        when(priceRepo.findByCustomerIdAndBranchIdAndProductIdIn(eq(customerId), eq(branchId), anyCollection()))
                .thenReturn(List.of());

        assertEquals(1, priceService.applyFromInvoice(invoice, userId));

        CustomerProductPrice saved = singleSavedPrice();
        assertEquals(customerId, saved.getCustomerId());
        assertEquals(1L, saved.getProductId());
        assertEquals(branchId, saved.getBranchId());
        assertEquals(0, new BigDecimal("95").compareTo(saved.getUnitPrice()));
        assertEquals(invoice.getId(), saved.getSourceInvoiceId());
        assertNotNull(saved.getEffectiveFrom());
        assertNull(saved.getEffectiveTo());

        CustomerProductPriceHistory h = singleSavedHistory();
        assertNull(h.getOldPrice());
        assertEquals(0, new BigDecimal("95").compareTo(h.getNewPrice()));
        assertEquals(0, new BigDecimal("100").compareTo(h.getListPrice()));
        assertEquals(userId, h.getChangedBy());
    }

    @Test
    void applyFromInvoice_DoesNothing_WhenPriceEqualsListAndNoCustomerPrice() {
        SalesInvoice invoice = invoice(line(1L, "100.0000"));
        when(catalogFacade.getListPrices(eq(branchId), anyCollection())).thenReturn(Map.of(1L, new BigDecimal("100")));
        when(priceRepo.findByCustomerIdAndBranchIdAndProductIdIn(eq(customerId), eq(branchId), anyCollection()))
                .thenReturn(List.of());

        assertEquals(0, priceService.applyFromInvoice(invoice, userId));
        verify(priceRepo, never()).saveAll(any());
        verify(historyRepo, never()).saveAll(any());
    }

    @Test
    void applyFromInvoice_DoesNothing_WhenPriceEqualsActiveCustomerPrice() {
        SalesInvoice invoice = invoice(line(1L, "90"));
        when(catalogFacade.getListPrices(eq(branchId), anyCollection())).thenReturn(Map.of(1L, new BigDecimal("100")));
        when(priceRepo.findByCustomerIdAndBranchIdAndProductIdIn(eq(customerId), eq(branchId), anyCollection()))
                .thenReturn(List.of(customerPrice(1L, "90")));

        assertEquals(0, priceService.applyFromInvoice(invoice, userId));
        verify(priceRepo, never()).saveAll(any());
    }

    @Test
    void applyFromInvoice_UpdatesActiveCustomerPrice_EvenBackToListPrice() {
        CustomerProductPrice existing = customerPrice(1L, "90");
        SalesInvoice invoice = invoice(line(1L, "100"));
        when(catalogFacade.getListPrices(eq(branchId), anyCollection())).thenReturn(Map.of(1L, new BigDecimal("100")));
        when(priceRepo.findByCustomerIdAndBranchIdAndProductIdIn(eq(customerId), eq(branchId), anyCollection()))
                .thenReturn(List.of(existing));

        assertEquals(1, priceService.applyFromInvoice(invoice, userId));

        CustomerProductPrice saved = singleSavedPrice();
        assertSame(existing, saved, "phải cập nhật bản ghi cũ (unique customer+product+branch)");
        assertEquals(0, new BigDecimal("100").compareTo(saved.getUnitPrice()));
        assertEquals(0, new BigDecimal("90").compareTo(singleSavedHistory().getOldPrice()));
    }

    @Test
    void applyFromInvoice_ReactivatesExpiredRow_WhenPriceDiffersFromList() {
        CustomerProductPrice expired = customerPrice(1L, "80");
        expired.setEffectiveTo(LocalDateTime.now().minusDays(1));
        SalesInvoice invoice = invoice(line(1L, "85"));
        when(catalogFacade.getListPrices(eq(branchId), anyCollection())).thenReturn(Map.of(1L, new BigDecimal("100")));
        when(priceRepo.findByCustomerIdAndBranchIdAndProductIdIn(eq(customerId), eq(branchId), anyCollection()))
                .thenReturn(List.of(expired));

        assertEquals(1, priceService.applyFromInvoice(invoice, userId));

        CustomerProductPrice saved = singleSavedPrice();
        assertSame(expired, saved);
        assertNull(saved.getEffectiveTo());
        assertTrue(saved.isActiveAt(LocalDateTime.now().plusSeconds(1)));
        assertNull(singleSavedHistory().getOldPrice(), "giá cũ đã hết hạn -> coi như chưa có giá riêng");
    }

    @Test
    void applyFromInvoice_SavesManualPrice_WhenNoListPrice() {
        SalesInvoice invoice = invoice(line(1L, "42"));
        when(catalogFacade.getListPrices(eq(branchId), anyCollection())).thenReturn(Map.of());
        when(priceRepo.findByCustomerIdAndBranchIdAndProductIdIn(eq(customerId), eq(branchId), anyCollection()))
                .thenReturn(List.of());

        assertEquals(1, priceService.applyFromInvoice(invoice, userId));
        assertEquals(0, new BigDecimal("42").compareTo(singleSavedPrice().getUnitPrice()));
        assertNull(singleSavedHistory().getListPrice());
    }

    @Test
    void applyFromInvoice_SameProductOnSeveralLines_UsesLastLine() {
        SalesInvoice invoice = invoice(line(1L, "95"), line(2L, "200"), line(1L, "97"));
        when(catalogFacade.getListPrices(eq(branchId), anyCollection()))
                .thenReturn(Map.of(1L, new BigDecimal("100"), 2L, new BigDecimal("200")));
        when(priceRepo.findByCustomerIdAndBranchIdAndProductIdIn(eq(customerId), eq(branchId), anyCollection()))
                .thenReturn(List.of());

        assertEquals(1, priceService.applyFromInvoice(invoice, userId));
        CustomerProductPrice saved = singleSavedPrice();
        assertEquals(1L, saved.getProductId());
        assertEquals(0, new BigDecimal("97").compareTo(saved.getUnitPrice()));
    }

    // ------------------------------------------------------------------ helpers

    private CustomerProductPrice customerPrice(Long productId, String price) {
        CustomerProductPrice p = new CustomerProductPrice();
        p.setCustomerId(customerId);
        p.setProductId(productId);
        p.setBranchId(branchId);
        p.setUnitPrice(new BigDecimal(price));
        p.setEffectiveFrom(LocalDateTime.now().minusDays(10));
        return p;
    }

    private SalesInvoiceLine line(Long productId, String unitPrice) {
        SalesInvoiceLine l = new SalesInvoiceLine();
        l.setProductId(productId);
        l.setUnitPrice(new BigDecimal(unitPrice));
        l.setQuantity(BigDecimal.ONE);
        l.setLineTotal(new BigDecimal(unitPrice));
        l.setUnitOfMeasure("CAI");
        return l;
    }

    private SalesInvoice invoice(SalesInvoiceLine... lines) {
        SalesInvoice inv = new SalesInvoice();
        ReflectionTestUtils.setField(inv, "id", UUID.randomUUID());
        inv.setInvoiceCode("HD-PRICE");
        inv.setCustomerId(customerId);
        inv.setBranchId(branchId);
        for (SalesInvoiceLine l : lines) inv.addLine(l);
        return inv;
    }

    @SuppressWarnings("unchecked")
    private CustomerProductPrice singleSavedPrice() {
        ArgumentCaptor<Iterable<CustomerProductPrice>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(priceRepo).saveAll(captor.capture());
        List<CustomerProductPrice> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertEquals(1, saved.size());
        return saved.get(0);
    }

    @SuppressWarnings("unchecked")
    private CustomerProductPriceHistory singleSavedHistory() {
        ArgumentCaptor<Iterable<CustomerProductPriceHistory>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(historyRepo).saveAll(captor.capture());
        List<CustomerProductPriceHistory> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertEquals(1, saved.size());
        return saved.get(0);
    }

    private static void assertResolved(ResolvedPriceDto dto, Long productId, String price, PriceSource source, String listPrice) {
        assertEquals(productId, dto.productId());
        assertEquals(source, dto.source());
        assertEquals(0, new BigDecimal(price).compareTo(dto.unitPrice()));
        assertEquals(0, new BigDecimal(listPrice).compareTo(dto.listPrice()));
    }
}
