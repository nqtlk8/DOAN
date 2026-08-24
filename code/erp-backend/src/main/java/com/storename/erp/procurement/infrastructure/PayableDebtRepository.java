package com.storename.erp.procurement.infrastructure;

import com.storename.erp.procurement.domain.PayableDebt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayableDebtRepository extends JpaRepository<PayableDebt, UUID> {
    Optional<PayableDebt> findBySupplierIdAndBranchId(UUID supplierId, UUID branchId);
}
