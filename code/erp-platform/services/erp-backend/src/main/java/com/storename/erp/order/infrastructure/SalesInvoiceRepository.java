package com.storename.erp.order.infrastructure;

import com.storename.erp.order.domain.SalesInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, UUID> {
    Optional<SalesInvoice> findByInvoiceCode(String invoiceCode);
    java.util.List<SalesInvoice> findByBranchId(Long branchId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT i FROM SalesInvoice i WHERE i.id = :id")
    Optional<SalesInvoice> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
}
