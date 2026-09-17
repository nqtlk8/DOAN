package com.storename.erp.crm.infrastructure;

import com.storename.erp.crm.domain.ReceivableDebtMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReceivableDebtMovementRepository extends JpaRepository<ReceivableDebtMovement, UUID> {
    boolean existsByCustomerIdAndBranchId(UUID customerId, Long branchId);
    
    java.util.List<ReceivableDebtMovement> findByCustomerIdAndBranchIdOrderByCreatedAtDesc(UUID customerId, Long branchId);
}
