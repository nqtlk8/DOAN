package com.storename.erp.inventory.infrastructure;

import com.storename.erp.inventory.domain.InboundReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InboundReceiptRepository extends JpaRepository<InboundReceipt, UUID> {
    boolean existsByReceiptCode(String receiptCode);
    java.util.List<InboundReceipt> findByBranchId(Long branchId);
}
