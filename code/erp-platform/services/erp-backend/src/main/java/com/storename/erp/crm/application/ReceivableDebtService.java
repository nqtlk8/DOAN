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
        ReceivableDebt debt = debtRepository.findByCustomerIdAndBranchId(customerId, branchId)
                .orElseGet(() -> createDebtRecord(customerId, branchId));
        debt.increaseDebt(amount);
        ReceivableDebt saved = debtRepository.save(debt);
        
        recordMovement(saved.getCustomer(), branchId, type, amount, saved.getTotalDebt(), refType, refId, userId, note);
        return saved;
    }

    @Transactional
    public ReceivableDebt decreaseDebt(UUID customerId, Long branchId, BigDecimal amount,
                                       ReceivableDebtMovementType type, String refType, String refId, 
                                       UUID userId, String note) {
        ReceivableDebt debt = debtRepository.findByCustomerIdAndBranchId(customerId, branchId)
                .orElseThrow(() -> new RuntimeException("Debt record not found"));
        debt.decreaseDebt(amount);
        ReceivableDebt saved = debtRepository.save(debt);
        
        // Use negative amount for decrease in movement to signify it was a decrease? 
        // Or positive amount but the movementType implies it?
        // Let's use positive amount, and movementType INVOICE increases, PAYMENT/RETURN decreases.
        // Wait, it might be better to just store positive amount for now and rely on movementType, 
        // or just use negative. Let's use negative for decrease for simpler sum calculations later if needed.
        // Actually, the amount is absolute. Balance After shows the direction. Let's keep it positive.
        recordMovement(debt.getCustomer(), branchId, type, amount, saved.getTotalDebt(), refType, refId, userId, note);
        return saved;
    }
    
    @Transactional
    public ReceivableDebt setOpeningBalance(UUID customerId, Long branchId, BigDecimal amount, UUID userId, String note) {
        if (movementRepository.existsByCustomerIdAndBranchId(customerId, branchId)) {
            throw new RuntimeException("Cannot set opening balance because movements already exist for this customer.");
        }

        ReceivableDebt debt = debtRepository.findByCustomerIdAndBranchId(customerId, branchId)
                .orElseGet(() -> createDebtRecord(customerId, branchId));
        
        debt.setTotalDebt(amount);
        ReceivableDebt saved = debtRepository.save(debt);
        
        recordMovement(saved.getCustomer(), branchId, ReceivableDebtMovementType.OPENING_BALANCE, amount, amount, 
                       "OPENING_BALANCE", "INIT", userId, note);
        return saved;
    }

    private void recordMovement(Customer customer, Long branchId, ReceivableDebtMovementType type, BigDecimal amount, 
                                BigDecimal balanceAfter, String refType, String refId, UUID userId, String note) {
        ReceivableDebtMovement movement = new ReceivableDebtMovement();
        movement.setBranchId(branchId);
        movement.setCustomer(customer);
        movement.setMovementType(type);
        movement.setAmount(amount);
        movement.setBalanceAfter(balanceAfter);
        movement.setRefType(refType);
        movement.setRefId(refId);
        movement.setCreatedBy(userId);
        movement.setNote(note);
        movementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentDebt(UUID customerId, Long branchId) {
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
        return movementRepository.findByCustomerIdAndBranchIdOrderByCreatedAtDesc(customerId, branchId);
    }
}
