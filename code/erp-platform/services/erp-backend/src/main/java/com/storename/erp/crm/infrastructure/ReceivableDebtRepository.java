package com.storename.erp.crm.infrastructure;

import com.storename.erp.crm.domain.ReceivableDebt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceivableDebtRepository extends JpaRepository<ReceivableDebt, UUID> {
    Optional<ReceivableDebt> findByCustomerIdAndBranchId(UUID customerId, Long branchId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(r.totalDebt) FROM ReceivableDebt r WHERE (:branchId IS NULL OR r.branchId = :branchId)")
    java.math.BigDecimal getTotalDebt(@org.springframework.data.repository.query.Param("branchId") Long branchId);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"customer"})
    java.util.List<ReceivableDebt> findByBranchId(Long branchId);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"customer"})
    java.util.List<ReceivableDebt> findByCustomerId(UUID customerId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"customer"})
    java.util.List<ReceivableDebt> findAll();
}
