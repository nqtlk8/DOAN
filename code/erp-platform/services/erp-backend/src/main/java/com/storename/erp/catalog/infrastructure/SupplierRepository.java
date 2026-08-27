package com.storename.erp.catalog.infrastructure;

import com.storename.erp.catalog.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List; import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findByBranchId(Long branchId);
}
