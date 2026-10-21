package com.storename.erp.crm.application;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.domain.ReceivableDebt;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.crm.infrastructure.ReceivableDebtRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import com.storename.erp.crm.domain.ReceivableDebtMovement;
import com.storename.erp.crm.domain.ReceivableDebtMovementType;
import com.storename.erp.crm.infrastructure.ReceivableDebtMovementRepository;

@Service
@Slf4j
public class ReceivableDebtService {

    /** Mã lỗi (HTTP 409): thao tác làm công nợ âm (giảm nợ vượt số dư hiện có). */
    public static final String DEBT_NEGATIVE = "DEBT_NEGATIVE";

    private final ReceivableDebtRepository debtRepository;
    private final CustomerRepository customerRepository;
    private final ReceivableDebtMovementRepository movementRepository;

    public ReceivableDebtService(ReceivableDebtRepository debtRepository, CustomerRepository customerRepository, ReceivableDebtMovementRepository movementRepository) {
        this.debtRepository = debtRepository;
        this.customerRepository = customerRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public ReceivableDebt increaseDebt(UUID customerId, Long branchId, BigDecimal amount, 
                                       ReceivableDebtMovementType type, String refType, String refId, 
                                       UUID userId, String note) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("increaseDebt requires a positive amount");
        }
        return processDebtChange(customerId, branchId, amount, type, refType, refId, userId, note);
    }

    @Transactional
    public ReceivableDebt decreaseDebt(UUID customerId, Long branchId, BigDecimal amount,
                                       ReceivableDebtMovementType type, String refType, String refId, 
                                       UUID userId, String note) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("decreaseDebt requires a positive amount");
        }
        // Negate amount for decrease
        BigDecimal signedAmount = amount.negate();
        return processDebtChange(customerId, branchId, signedAmount, type, refType, refId, userId, note);
    }

    /**
     * Giảm công nợ tối đa bằng số dư hiện có (không làm công nợ âm) và trả về số tiền đã trừ thật.
     * Dùng cho trả hàng: khách đã trả đủ (công nợ 0) vẫn trả hàng được, phần không trừ được vào
     * công nợ là tiền phải hoàn lại cho khách. Không ghi biến động khi số tiền trừ được bằng 0.
     */
    @Transactional
    public BigDecimal decreaseDebtUpToBalance(UUID customerId, Long branchId, BigDecimal amount,
                                              ReceivableDebtMovementType type, String refType, String refId,
                                              UUID userId, String note) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("decreaseDebtUpToBalance requires a positive amount");
        }
        BigDecimal balance = debtRepository.findForUpdate(customerId, branchId)
                .map(ReceivableDebt::getTotalDebt)
                .orElse(BigDecimal.ZERO);
        BigDecimal applied = amount.min(balance.max(BigDecimal.ZERO));
        if (applied.compareTo(BigDecimal.ZERO) > 0) {
            processDebtChange(customerId, branchId, applied.negate(), type, refType, refId, userId, note);
        }
        return applied;
    }

    private ReceivableDebt processDebtChange(UUID customerId, Long branchId, BigDecimal signedAmount,
                                             ReceivableDebtMovementType type, String refType, String refId, 
                                             UUID userId, String note) {
        ReceivableDebt debt = debtRepository.findForUpdate(customerId, branchId)
                .orElseGet(() -> createDebtRecord(customerId, branchId));
                
        BigDecimal balanceBefore = debt.getTotalDebt();
        BigDecimal balanceAfter = balanceBefore.add(signedAmount);
        
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessConflictException(DEBT_NEGATIVE, String.format(
                    "Công nợ không được âm: số dư hiện tại %s, số tiền giảm %s",
                    balanceBefore.stripTrailingZeros().toPlainString(),
                    signedAmount.negate().stripTrailingZeros().toPlainString()));
        }
        
        debt.setTotalDebt(balanceAfter);
        ReceivableDebt saved = debtRepository.save(debt);
        
        recordMovement(saved.getCustomer(), branchId, type, signedAmount, balanceBefore, balanceAfter, refType, refId, userId, note);
        return saved;
    }

    private void recordMovement(Customer customer, Long branchId, ReceivableDebtMovementType type, BigDecimal amount, 
                                BigDecimal balanceBefore, BigDecimal balanceAfter, String refType, String refId, UUID userId, String note) {
        String idempotencyKey = type.name() + ":" + refType + ":" + refId;

        ReceivableDebtMovement movement = ReceivableDebtMovement.create(
                branchId, customer, type, amount, balanceBefore, balanceAfter, refType, refId, userId, note, idempotencyKey
        );
        movementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentDebt(UUID customerId, Long branchId) {
        if (branchId == null) {
            return debtRepository.findByCustomerId(customerId).stream()
                    .map(ReceivableDebt::getTotalDebt)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return debtRepository.findByCustomerIdAndBranchId(customerId, branchId)
                .map(ReceivableDebt::getTotalDebt)
                .orElse(BigDecimal.ZERO);
    }

    private ReceivableDebt createDebtRecord(UUID customerId, Long branchId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new com.storename.erp.common.exception.ResourceNotFoundException("Khách hàng chưa có tại chi nhánh (chưa đồng bộ từ HQ): " + customerId));
        ReceivableDebt debt = new ReceivableDebt();
        debt.setCustomer(customer);
        debt.setBranchId(branchId);
        debt.setTotalDebt(BigDecimal.ZERO);
        return debt;
    }

    @Transactional(readOnly = true)
    public java.util.List<ReceivableDebtMovement> getMovements(UUID customerId, Long branchId) {
        if (branchId == null) {
            return movementRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        }
        return movementRepository.findByCustomerIdAndBranchIdOrderByCreatedAtDesc(customerId, branchId);
    }
}
