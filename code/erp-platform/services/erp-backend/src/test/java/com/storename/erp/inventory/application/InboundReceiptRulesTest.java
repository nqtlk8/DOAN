package com.storename.erp.inventory.application;

import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.inventory.domain.InboundReceipt;
import com.storename.erp.inventory.domain.InboundReceiptLine;
import com.storename.erp.inventory.domain.StockMovement;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.CostLayerRepository;
import com.storename.erp.inventory.infrastructure.InboundReceiptRepository;
import com.storename.erp.inventory.infrastructure.StockMovementRepository;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Khóa dòng tồn, tạo dòng tồn mới, mã lỗi và phạm vi chi nhánh của InboundReceiptService. */
@ExtendWith(MockitoExtension.class)
class InboundReceiptRulesTest {

    @Mock private InboundReceiptRepository inboundRepo;
    @Mock private StockOnHandRepository stockRepo;
    @Mock private StockMovementRepository movementRepo;
    @Mock private CostLayerRepository costLayerRepo;

    @InjectMocks private InboundReceiptService service;

    private final Long branchId = 1L;
    private final UUID receiptId = UUID.randomUUID();

    private InboundReceipt receipt(long... productIds) {
        InboundReceipt r = new InboundReceipt(branchId, "NK-T", null);
        ReflectionTestUtils.setField(r, "id", receiptId);
        for (long pid : productIds) {
            InboundReceiptLine line = new InboundReceiptLine(pid, new BigDecimal("5"), new BigDecimal("100"), "Cai");
            ReflectionTestUtils.setField(line, "id", UUID.randomUUID());
            r.addLine(line);
        }
        return r;
    }

    private void stubMovementSave() {
        when(movementRepo.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement m = i.getArgument(0);
            ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
            return m;
        });
    }

    @Test
    @DisplayName("Khóa dòng tồn theo productId tăng dần; chưa có dòng tồn thì tạo và flush ngay")
    void confirm_locksInProductIdOrder_andCreatesMissingRow() {
        InboundReceipt r = receipt(30L, 10L);
        when(inboundRepo.findByIdForUpdate(receiptId)).thenReturn(Optional.of(r));
        StockOnHand existing = new StockOnHand(10L, branchId);
        when(stockRepo.findForUpdate(10L, branchId)).thenReturn(Optional.of(existing));
        when(stockRepo.findForUpdate(30L, branchId)).thenReturn(Optional.empty());
        when(stockRepo.saveAndFlush(any(StockOnHand.class))).thenAnswer(i -> i.getArgument(0));
        stubMovementSave();

        service.confirmReceipt(receiptId, branchId, UUID.randomUUID());

        InOrder order = inOrder(stockRepo);
        order.verify(stockRepo).findForUpdate(10L, branchId);
        order.verify(stockRepo).findForUpdate(30L, branchId);
        verify(stockRepo).saveAndFlush(argThat(s -> s.getProductId().equals(30L)));
        verify(stockRepo, never()).findByProductIdAndBranchId(any(), any());
        assertThat(existing.getQuantity()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("Không tìm thấy -> 404; khác chi nhánh -> 403; không có chi nhánh -> 403")
    void errors() {
        when(inboundRepo.findByIdForUpdate(receiptId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirmReceipt(receiptId, branchId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        when(inboundRepo.findByIdForUpdate(receiptId)).thenReturn(Optional.of(receipt(1L)));
        assertThatThrownBy(() -> service.confirmReceipt(receiptId, 2L, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> service.confirmReceipt(receiptId, null, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Xác nhận phiếu đã CONFIRMED -> 409 INVALID_STATUS, không tạo tồn lần hai")
    void confirmTwice_is409() {
        InboundReceipt r = receipt(1L);
        r.confirm(UUID.randomUUID());
        when(inboundRepo.findByIdForUpdate(receiptId)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.confirmReceipt(receiptId, branchId, UUID.randomUUID()))
                .isInstanceOf(BusinessConflictException.class)
                .extracting("code").isEqualTo("INVALID_STATUS");
        verifyNoInteractions(stockRepo, movementRepo, costLayerRepo);
    }

    @Test
    @DisplayName("ADMIN HQ (branchId null): danh sách là mọi phiếu nhập, không phải danh sách rỗng")
    void listWithoutBranch_returnsAll() {
        when(inboundRepo.findAll()).thenReturn(List.of(receipt(1L)));

        assertThat(service.getReceiptsByBranch(null)).hasSize(1);
        verify(inboundRepo, never()).findByBranchId(any());
    }

    @Test
    @DisplayName("ADMIN HQ xem chi tiết phiếu của chi nhánh bất kỳ; STAFF chi nhánh khác -> 403")
    void getReceipt_branchRules() {
        when(inboundRepo.findById(receiptId)).thenReturn(Optional.of(receipt(1L)));

        assertThat(service.getReceipt(receiptId, null)).isNotNull();
        assertThatThrownBy(() -> service.getReceipt(receiptId, 2L)).isInstanceOf(AccessDeniedException.class);
        verify(inboundRepo, times(2)).findById(eq(receiptId));
    }
}
