package com.storename.erp.order.infrastructure;

import com.storename.erp.order.domain.CustomerProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerProductPriceHistoryRepository extends JpaRepository<CustomerProductPriceHistory, UUID> {
    List<CustomerProductPriceHistory> findByCustomerIdAndProductIdAndBranchIdOrderByChangedAtDesc(UUID customerId, Long productId, Long branchId);
}
