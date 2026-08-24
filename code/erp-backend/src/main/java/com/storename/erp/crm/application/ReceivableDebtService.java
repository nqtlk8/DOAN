package com.storename.erp.crm.application;

import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.domain.ReceivableDebt;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.crm.infrastructure.ReceivableDebtRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ReceivableDebtService {

    private final ReceivableDebtRepository debtRepository;
    private final CustomerRepository customerRepository;

    public ReceivableDebtService(ReceivableDebtRepository debtRepository, CustomerRepository customerRepository) {
        this.debtRepository = debtRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public ReceivableDebt increaseDebt(UUID customerId, UUID branchId, BigDecimal amount) {
        ReceivableDebt debt = debtRepository.findByCustomerIdAndBranchId(customerId, branchId)
                .orElseGet(() -> createDebtRecord(customerId, branchId));
        debt.increaseDebt(amount);
        return debtRepository.save(debt);
    }

    @Transactional
    public ReceivableDebt decreaseDebt(UUID customerId, UUID branchId, BigDecimal amount) {
        ReceivableDebt debt = debtRepository.findByCustomerIdAndBranchId(customerId, branchId)
                .orElseThrow(() -> new RuntimeException("Debt record not found"));
        debt.decreaseDebt(amount);
        return debtRepository.save(debt);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentDebt(UUID customerId, UUID branchId) {
        return debtRepository.findByCustomerIdAndBranchId(customerId, branchId)
                .map(ReceivableDebt::getTotalDebt)
                .orElse(BigDecimal.ZERO);
    }

    private ReceivableDebt createDebtRecord(UUID customerId, UUID branchId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        ReceivableDebt debt = new ReceivableDebt();
        debt.setCustomer(customer);
        debt.setBranchId(branchId);
        debt.setTotalDebt(BigDecimal.ZERO);
        return debt;
    }
}
