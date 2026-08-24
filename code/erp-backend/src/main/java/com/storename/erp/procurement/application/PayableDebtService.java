package com.storename.erp.procurement.application;

import com.storename.erp.procurement.domain.PayableDebt;
import com.storename.erp.procurement.domain.Supplier;
import com.storename.erp.procurement.infrastructure.PayableDebtRepository;
import com.storename.erp.procurement.infrastructure.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service quản lý công nợ phải trả cho nhà cung cấp (Payable Debt).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayableDebtService {

    private final PayableDebtRepository debtRepository;
    private final SupplierRepository supplierRepository;

    /**
     * Tăng công nợ nhà cung cấp.
     * @param supplierId ID nhà cung cấp
     * @param branchId ID chi nhánh
     * @param amount Số tiền tăng
     */
    @Transactional
    public void increaseDebt(UUID supplierId, UUID branchId, BigDecimal amount) {
        PayableDebt debt = getOrCreateDebt(supplierId, branchId);
        debt.increaseDebt(amount);
        debtRepository.save(debt);
        log.info("Increased debt for supplier {} at branch {} by amount {}", supplierId, branchId, amount);
    }

    /**
     * Giảm công nợ nhà cung cấp.
     * @param supplierId ID nhà cung cấp
     * @param branchId ID chi nhánh
     * @param amount Số tiền giảm
     */
    @Transactional
    public void decreaseDebt(UUID supplierId, UUID branchId, BigDecimal amount) {
        PayableDebt debt = getOrCreateDebt(supplierId, branchId);
        debt.decreaseDebt(amount);
        debtRepository.save(debt);
        log.info("Decreased debt for supplier {} at branch {} by amount {}", supplierId, branchId, amount);
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentDebt(UUID supplierId, UUID branchId) {
        return debtRepository.findBySupplierIdAndBranchId(supplierId, branchId)
                .map(PayableDebt::getTotalDebt)
                .orElse(BigDecimal.ZERO);
    }

    private PayableDebt getOrCreateDebt(UUID supplierId, UUID branchId) {
        return debtRepository.findBySupplierIdAndBranchId(supplierId, branchId)
                .orElseGet(() -> {
                    Supplier supplier = supplierRepository.findById(supplierId)
                            .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
                    PayableDebt newDebt = new PayableDebt();
                    newDebt.setSupplier(supplier);
                    newDebt.setBranchId(branchId);
                    newDebt.setTotalDebt(BigDecimal.ZERO);
                    return debtRepository.save(newDebt);
                });
    }
}
