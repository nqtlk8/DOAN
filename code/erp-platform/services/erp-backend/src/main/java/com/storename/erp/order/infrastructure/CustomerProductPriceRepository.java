package com.storename.erp.order.infrastructure;

import com.storename.erp.order.domain.CustomerProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerProductPriceRepository extends JpaRepository<CustomerProductPrice, UUID> {
    Optional<CustomerProductPrice> findByCustomerIdAndProductIdAndBranchId(UUID customerId, Long productId, Long branchId);
}
