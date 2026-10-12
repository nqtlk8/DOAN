package com.storename.erp.order.application;

import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.crm.api.CrmFacade;
import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.inventory.api.InventoryFacade;
import com.storename.erp.order.domain.GoodsReturn;
import com.storename.erp.order.domain.GoodsReturnLine;
import com.storename.erp.order.domain.GoodsReturnStatus;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.domain.SalesInvoiceStatus;
import com.storename.erp.order.infrastructure.GoodsReturnRepository;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Quy tắc xác nhận phiếu trả hàng sau khi sửa:
 * cộng dồn số lượng theo (sản phẩm, ĐVT) trong phiếu và trên hóa đơn gốc, giá vốn bình quân khi
 * hóa đơn có nhiều dòng cùng sản phẩm, hoàn tiền khi công nợ không đủ, lỗi 4xx đúng loại.
 */
@ExtendWith(MockitoExtension.class)
class GoodsReturnRulesTest {

    @Mock private GoodsReturnRepository returnRepository;
    @Mock private ReceivableDebtService debtService;
    @Mock private SalesInvoiceRepository invoiceRepository;
    @Mock private InventoryFacade inventoryFacade;
    @Mock private CrmFacade crmFacade;

    @InjectMocks private GoodsReturnService service;

    private final Long branchId = 1L;
    private final UUID customerId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();
    private final UUID returnId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private SalesInvoice invoice;
    private GoodsReturn goodsReturn;

    @BeforeEach
    void setUp() {
        invoice = new SalesInvoice();
        ReflectionTestUtils.setField(invoice, "id", invoiceId);
        invoice.setBranchId(branchId);
        invoice.setCustomerId(customerId);
        invoice.setStatus(SalesInvoiceStatus.CONFIRMED);

        goodsReturn = new GoodsReturn();
        ReflectionTestUtils.setField(goodsReturn, "id", returnId);
        goodsReturn.setReturnCode("TH-TEST");
        goodsReturn.setBranchId(branchId);
        goodsReturn.setCustomerId(customerId);
        goodsReturn.setInvoiceId(invoiceId);
    }

    private void invoiceLine(long productId, String uom, String qty, String unitCost) {
        SalesInvoiceLine l = new SalesInvoiceLine();
        l.setProductId(productId);
        l.setUnitOfMeasure(uom);
        l.setQuantity(new BigDecimal(qty));
        l.setUnitPrice(new BigDecimal("100"));
        l.setUnitCost(unitCost == null ? null : new BigDecimal(unitCost));
        invoice.addLine(l);
    }

    private GoodsReturnLine returnLine(long productId, String uom, String qty, String unitPrice) {
        GoodsReturnLine l = new GoodsReturnLine();
        ReflectionTestUtils.setField(l, "id", UUID.randomUUID());
        l.setProductId(productId);
        l.setUnitOfMeasure(uom);
        l.setQuantity(new BigDecimal(qty));
        l.setUnitPrice(new BigDecimal(unitPrice));
        goodsReturn.addLine(l);
        goodsReturn.calculateTotal();
        return l;
    }

    private void stubFound() {
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(goodsReturn));
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
    }

    private void stubDebtFullyApplied() {
        when(debtService.decreaseDebtUpToBalance(eq(customerId), eq(branchId), any(), any(), anyString(), anyString(), any(), anyString()))
                .thenAnswer(i -> i.getArgument(2));
    }

    @Test
    @DisplayName("Hai dòng cùng sản phẩm trong một phiếu được cộng dồn: 6 + 6 > 10 đã bán -> từ chối, không hoàn kho")
    void duplicateLinesInSameReturn_areAggregated() {
        invoiceLine(10L, "Cai", "10", "30");
        returnLine(10L, "Cai", "6", "100");
        returnLine(10L, "Cai", "6", "100");
        stubFound();
        when(returnRepository.getTotalReturnedQuantity(invoiceId, 10L, "Cai")).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.confirmReturn(returnId, branchId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds invoice sold quantity");

        assertThat(goodsReturn.getStatus()).isEqualTo(GoodsReturnStatus.DRAFT);
        verifyNoInteractions(inventoryFacade, debtService);
    }

    @Test
    @DisplayName("Hóa đơn có hai dòng cùng sản phẩm (4 + 6): trả 8 hợp lệ vì so với tổng 10")
    void invoiceWithTwoLinesOfSameProduct_usesTotalSoldQuantity() {
        invoiceLine(10L, "Cai", "4", "30");
        invoiceLine(10L, "Cai", "6", "30");
        returnLine(10L, "Cai", "8", "100");
        stubFound();
        when(returnRepository.getTotalReturnedQuantity(invoiceId, 10L, "Cai")).thenReturn(BigDecimal.ZERO);
        stubDebtFullyApplied();

        service.confirmReturn(returnId, branchId, userId);

        assertThat(goodsReturn.getStatus()).isEqualTo(GoodsReturnStatus.CONFIRMED);
        verify(inventoryFacade).recordReturn(eq(10L), eq(branchId), argThat(q -> q.compareTo(new BigDecimal("8")) == 0),
                argThat(c -> c.compareTo(new BigDecimal("30")) == 0), anyString(), any(), eq(userId));
    }

    @Test
    @DisplayName("Số đã trả trước đó được cộng vào: đã trả 7, trả thêm 4 > 10 -> từ chối")
    void previousReturnsAreCounted() {
        invoiceLine(10L, "Cai", "10", "30");
        returnLine(10L, "Cai", "4", "100");
        stubFound();
        when(returnRepository.getTotalReturnedQuantity(invoiceId, 10L, "Cai")).thenReturn(new BigDecimal("7"));

        assertThatThrownBy(() -> service.confirmReturn(returnId, branchId, userId))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(inventoryFacade);
    }

    @Test
    @DisplayName("Sản phẩm/ĐVT không có trên hóa đơn gốc -> từ chối")
    void productNotOnInvoice_isRejected() {
        invoiceLine(10L, "Cai", "10", "30");
        returnLine(10L, "Thung", "1", "100");
        stubFound();

        assertThatThrownBy(() -> service.confirmReturn(returnId, branchId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found in original invoice");
    }

    @Test
    @DisplayName("Giá vốn hàng trả = bình quân theo SL các dòng hóa đơn khớp: (4 x 30 + 6 x 40) / 10 = 36")
    void unitCost_isWeightedAverageOfMatchingInvoiceLines() {
        invoiceLine(10L, "Cai", "4", "30");
        invoiceLine(10L, "Cai", "6", "40");
        GoodsReturnLine line = returnLine(10L, "Cai", "1", "100");

        BigDecimal cost = service.resolveReturnUnitCost(invoice, customerId, branchId, line);

        assertThat(cost).isEqualByComparingTo("36");
    }

    @Test
    @DisplayName("Khách đã trả đủ: công nợ chỉ trừ được 30 trong 100 -> refundAmount = 70, vẫn xác nhận")
    void insufficientDebt_recordsCashRefund() {
        invoiceLine(10L, "Cai", "10", "30");
        returnLine(10L, "Cai", "1", "100");
        stubFound();
        when(returnRepository.getTotalReturnedQuantity(invoiceId, 10L, "Cai")).thenReturn(BigDecimal.ZERO);
        when(debtService.decreaseDebtUpToBalance(eq(customerId), eq(branchId), any(), any(), anyString(), anyString(), any(), anyString()))
                .thenReturn(new BigDecimal("30"));

        service.confirmReturn(returnId, branchId, userId);

        assertThat(goodsReturn.getStatus()).isEqualTo(GoodsReturnStatus.CONFIRMED);
        assertThat(goodsReturn.getRefundAmount()).isEqualByComparingTo("70");
        verify(returnRepository).save(goodsReturn);
    }

    @Test
    @DisplayName("Không có hóa đơn gốc: hoàn kho như nhập hàng, không kiểm tra số lượng")
    void withoutInvoice_noQuantityCheck() {
        goodsReturn.setInvoiceId(null);
        returnLine(10L, "Cai", "1000", "0");
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(goodsReturn));
        when(inventoryFacade.getLatestLayerUnitCost(10L, branchId)).thenReturn(new BigDecimal("25"));
        when(invoiceRepository.findRecentSaleUnitCosts(any(), any(), any(), any())).thenReturn(java.util.List.of());
        stubDebtFullyApplied();

        service.confirmReturn(returnId, branchId, userId);

        verify(returnRepository, never()).getTotalReturnedQuantity(any(), any(), any());
        verify(inventoryFacade).recordReturn(eq(10L), eq(branchId), any(), argThat(c -> c.compareTo(new BigDecimal("25")) == 0),
                anyString(), any(), eq(userId));
    }

    @Test
    @DisplayName("Dòng trả được hoàn kho theo productId tăng dần (cùng thứ tự khóa với bán và nhập)")
    void linesAreProcessedInProductIdOrder() {
        goodsReturn.setInvoiceId(null);
        returnLine(30L, "Cai", "1", "10");
        returnLine(10L, "Cai", "1", "10");
        returnLine(20L, "Cai", "1", "10");
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(goodsReturn));
        when(invoiceRepository.findRecentSaleUnitCosts(any(), any(), any(), any())).thenReturn(java.util.List.of());
        stubDebtFullyApplied();

        service.confirmReturn(returnId, branchId, userId);

        InOrder order = inOrder(inventoryFacade);
        order.verify(inventoryFacade).recordReturn(eq(10L), any(), any(), any(), anyString(), any(), any());
        order.verify(inventoryFacade).recordReturn(eq(20L), any(), any(), any(), anyString(), any(), any());
        order.verify(inventoryFacade).recordReturn(eq(30L), any(), any(), any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Xác nhận lần hai -> BusinessConflictException INVALID_STATUS (HTTP 409)")
    void confirmTwice_isConflict() {
        goodsReturn.setInvoiceId(null);
        returnLine(10L, "Cai", "1", "10");
        goodsReturn.confirm(userId);
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(goodsReturn));

        assertThatThrownBy(() -> service.confirmReturn(returnId, branchId, userId))
                .isInstanceOf(BusinessConflictException.class)
                .extracting("code").isEqualTo("INVALID_STATUS");
    }

    @Test
    @DisplayName("Không tìm thấy -> 404; khác chi nhánh -> 403; token không có chi nhánh -> 403")
    void notFound_wrongBranch_noBranch() {
        when(returnRepository.findById(returnId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirmReturn(returnId, branchId, userId))
                .isInstanceOf(ResourceNotFoundException.class);

        when(returnRepository.findById(returnId)).thenReturn(Optional.of(goodsReturn));
        assertThatThrownBy(() -> service.confirmReturn(returnId, 2L, userId))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> service.confirmReturn(returnId, null, userId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("ADMIN HQ (branchId null) xem danh sách phiếu trả của mọi chi nhánh")
    void listWithoutBranch_returnsAll() {
        when(returnRepository.findAll()).thenReturn(java.util.List.of(goodsReturn));
        when(crmFacade.getCustomerNames(any())).thenReturn(java.util.Map.of());

        assertThat(service.getReturnsByBranch(null)).hasSize(1);
        verify(returnRepository, never()).findByBranchId(any());
    }
}
