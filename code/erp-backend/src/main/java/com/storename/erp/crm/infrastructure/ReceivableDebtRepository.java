package com.storename.erp.crm.infrastructure;

import com.storename.erp.crm.domain.ReceivableDebt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceivableDebtRepository extends JpaRepository<ReceivableDebt, UUID> {
    Optional<ReceivableDebt> findByCustomerIdAndBranchId(UUID customerId, UUID branchId);
}
