package com.storename.erp.crm.infrastructure;

import com.storename.erp.crm.domain.ReceivableDebtMovement;
import org.springframework.data.repository.Repository;

import java.util.UUID;

@org.springframework.stereotype.Repository
public interface ReceivableDebtMovementRepository extends Repository<ReceivableDebtMovement, UUID> {
    
    ReceivableDebtMovement save(ReceivableDebtMovement entity);
    
    boolean existsByCustomerIdAndBranchId(UUID customerId, Long branchId);
    
    @org.springframework.data.jpa.repository.Query("SELECT m FROM ReceivableDebtMovement m WHERE m.customer.id = :customerId AND m.branchId = :branchId ORDER BY m.createdAt DESC, m.id DESC")
    java.util.List<ReceivableDebtMovement> findByCustomerIdAndBranchIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("customerId") UUID customerId, @org.springframework.data.repository.query.Param("branchId") Long branchId);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM ReceivableDebtMovement m WHERE m.customer.id = :customerId ORDER BY m.createdAt DESC, m.id DESC")
    java.util.List<ReceivableDebtMovement> findByCustomerIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("customerId") UUID customerId);
}
