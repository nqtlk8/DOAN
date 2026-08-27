package com.storename.erp.analytics.infrastructure;

import com.storename.erp.analytics.domain.InventoryAlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryAlertLogRepository extends JpaRepository<InventoryAlertLog, Long> {
    Optional<InventoryAlertLog> findByProductIdAndBranchIdAndStatus(Long productId, Long branchId, String status);
}
