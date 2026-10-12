package com.storename.erp.crm.application;

import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.domain.ReceivableDebt;
import com.storename.erp.crm.domain.ReceivableDebtMovement;
import com.storename.erp.crm.domain.ReceivableDebtMovementType;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.crm.infrastructure.ReceivableDebtMovementRepository;
import com.storename.erp.crm.infrastructure.ReceivableDebtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Thu nợ độc lập và trừ công nợ tối đa bằng số dư (dùng khi trả hàng). */
@ExtendWith(MockitoExtension.class)
class ReceivableDebtPaymentTest {

    @Mock private ReceivableDebtRepository debtRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private ReceivableDebtMovementRepository movementRepository;

    @InjectMocks private ReceivableDebtService service;

    private final UUID customerId = UUID.randomUUID();
    private final Long branchId = 1L;
    private ReceivableDebt debt;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer();
        customer.setId(customerId);
        debt = new ReceivableDebt();
        debt.setCustomer(customer);
        debt.setBranchId(branchId);
        debt.setTotalDebt(new BigDecimal("500000"));
    }

    @Test
    @DisplayName("Thu nợ 200.000 trên nợ 500.000 -> còn 300.000, ghi PAYMENT / DEBT_PAYMENT")
    void recordPayment_reducesDebtAndWritesLedger() {
        when(debtRepository.findForUpdate(customerId, branchId)).thenReturn(Optional.of(debt));
        when(debtRepository.save(any(ReceivableDebt.class))).thenAnswer(i -> i.getArgument(0));

        ReceivableDebt result = service.recordPayment(customerId, branchId, new BigDecimal("200000"), UUID.randomUUID(), null);

        assertThat(result.getTotalDebt()).isEqualByComparingTo("300000");
        ArgumentCaptor<ReceivableDebtMovement> captor = ArgumentCaptor.forClass(ReceivableDebtMovement.class);
        verify(movementRepository).save(captor.capture());
        ReceivableDebtMovement m = captor.getValue();
        assertThat(m.getMovementType()).isEqualTo(ReceivableDebtMovementType.PAYMENT);
        assertThat(m.getRefType()).isEqualTo(ReceivableDebtService.REF_DEBT_PAYMENT);
        assertThat(m.getAmount()).isEqualByComparingTo("-200000");
        assertThat(m.getBalanceBefore()).isEqualByComparingTo("500000");
        assertThat(m.getBalanceAfter()).isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("Hai lần thu nợ sinh hai khóa idempotency khác nhau (refId mới mỗi lần)")
    void recordPayment_eachPaymentHasOwnReference() {
        when(debtRepository.findForUpdate(customerId, branchId)).thenReturn(Optional.of(debt));
        when(debtRepository.save(any(ReceivableDebt.class))).thenAnswer(i -> i.getArgument(0));

        service.recordPayment(customerId, branchId, new BigDecimal("100"), UUID.randomUUID(), "lần 1");
        service.recordPayment(customerId, branchId, new BigDecimal("100"), UUID.randomUUID(), "lần 2");

        ArgumentCaptor<ReceivableDebtMovement> captor = ArgumentCaptor.forClass(ReceivableDebtMovement.class);
        verify(movementRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getIdempotencyKey())
                .isNotEqualTo(captor.getAllValues().get(1).getIdempotencyKey());
    }

    @Test
    @DisplayName("Thu vượt số nợ -> 409 DEBT_NEGATIVE, không ghi gì")
    void recordPayment_overDebt_isConflict() {
        when(debtRepository.findForUpdate(customerId, branchId)).thenReturn(Optional.of(debt));

        assertThatThrownBy(() -> service.recordPayment(customerId, branchId, new BigDecimal("500001"), UUID.randomUUID(), null))
                .isInstanceOf(BusinessConflictException.class)
                .extracting("code").isEqualTo(ReceivableDebtService.DEBT_NEGATIVE);
        verify(debtRepository, never()).save(any());
        verifyNoInteractions(movementRepository);
    }

    @Test
    @DisplayName("Số tiền thu bằng 0 hoặc âm -> 400")
    void recordPayment_nonPositiveAmount_isBadRequest() {
        assertThatThrownBy(() -> service.recordPayment(customerId, branchId, BigDecimal.ZERO, UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.recordPayment(customerId, branchId, new BigDecimal("-1"), UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(debtRepository, movementRepository);
    }

    @Test
    @DisplayName("Trả hàng 800.000 khi nợ 500.000 -> chỉ trừ 500.000, số dư về 0")
    void decreaseUpToBalance_capsAtBalance() {
        when(debtRepository.findForUpdate(customerId, branchId)).thenReturn(Optional.of(debt));
        when(debtRepository.save(any(ReceivableDebt.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal applied = service.decreaseDebtUpToBalance(customerId, branchId, new BigDecimal("800000"),
                ReceivableDebtMovementType.RETURN, "GOODS_RETURN", UUID.randomUUID().toString(), UUID.randomUUID(), "trả hàng");

        assertThat(applied).isEqualByComparingTo("500000");
        assertThat(debt.getTotalDebt()).isEqualByComparingTo("0");
        verify(movementRepository).save(any(ReceivableDebtMovement.class));
    }

    @Test
    @DisplayName("Công nợ bằng 0 (hoặc chưa có dòng công nợ) -> trừ 0, không ghi biến động")
    void decreaseUpToBalance_zeroBalance_writesNothing() {
        when(debtRepository.findForUpdate(customerId, branchId)).thenReturn(Optional.empty());

        BigDecimal applied = service.decreaseDebtUpToBalance(customerId, branchId, new BigDecimal("100"),
                ReceivableDebtMovementType.RETURN, "GOODS_RETURN", UUID.randomUUID().toString(), UUID.randomUUID(), "trả hàng");

        assertThat(applied).isEqualByComparingTo("0");
        verify(debtRepository, never()).save(any());
        verifyNoInteractions(movementRepository);
    }

    @Test
    @DisplayName("Đặt số dư đầu kỳ khi đã có biến động -> 409 OPENING_BALANCE_EXISTS")
    void openingBalance_afterMovements_isConflict() {
        when(movementRepository.existsByCustomerIdAndBranchId(customerId, branchId)).thenReturn(true);

        assertThatThrownBy(() -> service.setOpeningBalance(customerId, branchId, new BigDecimal("1"), UUID.randomUUID(), null))
                .isInstanceOf(BusinessConflictException.class)
                .extracting("code").isEqualTo(ReceivableDebtService.OPENING_BALANCE_EXISTS);
    }
}
