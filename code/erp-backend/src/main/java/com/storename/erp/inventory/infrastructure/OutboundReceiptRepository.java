package com.storename.erp.inventory.infrastructure;

import com.storename.erp.inventory.domain.OutboundReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OutboundReceiptRepository extends JpaRepository<OutboundReceipt, UUID> {
    boolean existsByReceiptCode(String receiptCode);
}
