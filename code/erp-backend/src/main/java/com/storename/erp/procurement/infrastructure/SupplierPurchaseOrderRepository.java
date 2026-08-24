package com.storename.erp.procurement.infrastructure;

import com.storename.erp.procurement.domain.SupplierPurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierPurchaseOrderRepository extends JpaRepository<SupplierPurchaseOrder, UUID> {
    Optional<SupplierPurchaseOrder> findByPoCode(String poCode);
    boolean existsByPoCode(String poCode);
}
