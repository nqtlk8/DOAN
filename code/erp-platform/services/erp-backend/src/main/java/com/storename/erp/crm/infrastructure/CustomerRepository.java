package com.storename.erp.crm.infrastructure;

import com.storename.erp.crm.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByCustomerCode(String customerCode);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Customer c WHERE :branchId IS NULL OR c.branchId = :branchId")
    java.util.List<Customer> findAllByBranchIdOrNull(@org.springframework.data.repository.query.Param("branchId") Long branchId);
}
