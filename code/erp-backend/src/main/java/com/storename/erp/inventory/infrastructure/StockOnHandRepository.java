package com.storename.erp.inventory.infrastructure;

import com.storename.erp.inventory.domain.StockOnHand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockOnHandRepository extends JpaRepository<StockOnHand, UUID> {
    Optional<StockOnHand> findByProductIdAndBranchId(UUID productId, UUID branchId);
}
