package com.storename.erp.crm.application;

import lombok.extern.slf4j.Slf4j;
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
    
    private ReceivableDebt processDebtChange(UUID customerId, Long branchId, BigDecimal signedAmount,
                                             ReceivableDebtMovementType type, String refType, String refId, 
                                             UUID userId, String note) {
        ReceivableDebt debt = debtRepository.findByCustomerIdAndBranchId(customerId, branchId)
                .orElseGet(() -> createDebtRecord(customerId, branchId));
                
        BigDecimal balanceBefore = debt.getTotalDebt();
        BigDecimal balanceAfter = balanceBefore.add(signedAmount);
        
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Receivable debt cannot become negative");
        }
        
        debt.setTotalDebt(balanceAfter);
        ReceivableDebt saved = debtRepository.save(debt);
        
        recordMovement(saved.getCustomer(), branchId, type, signedAmount, balanceBefore, balanceAfter, refType, refId, userId, note);
        return saved;
    }

    @Transactional
    public ReceivableDebt setOpeningBalance(UUID customerId, Long branchId, BigDecimal amount, UUID userId, String note) {
        if (movementRepository.existsByCustomerIdAndBranchId(customerId, branchId)) {
            throw new RuntimeException("Cannot set opening balance because movements already exist for this customer.");
        }

        ReceivableDebt debt = debtRepository.findByCustomerIdAndBranchId(customerId, branchId)
                .orElseGet(() -> createDebtRecord(customerId, branchId));
        
        BigDecimal balanceBefore = BigDecimal.ZERO;
        BigDecimal balanceAfter = amount;
        
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Receivable debt cannot become negative");
        }

        debt.setTotalDebt(balanceAfter);
        ReceivableDebt saved = debtRepository.save(debt);
        
        recordMovement(saved.getCustomer(), branchId, ReceivableDebtMovementType.OPENING_BALANCE, amount, balanceBefore, balanceAfter, 
                       "OPENING_BALANCE", "INIT", userId, note);
        return saved;
    }

    private void recordMovement(Customer customer, Long branchId, ReceivableDebtMovementType type, BigDecimal amount, 
                                BigDecimal balanceBefore, BigDecimal balanceAfter, String refType, String refId, UUID userId, String note) {
        String idempotencyKey = type.name() + ":" + refType + ":" + refId;
        if (type == ReceivableDebtMovementType.OPENING_BALANCE) {
            idempotencyKey += ":" + customer.getId() + ":" + branchId;
        }

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
                .orElseThrow(() -> new RuntimeException("Customer not found"));
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
