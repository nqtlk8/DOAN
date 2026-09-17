package com.storename.erp.catalog.infrastructure;

import com.storename.erp.catalog.domain.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceListRepository extends JpaRepository<PriceList, Long> {
    List<PriceList> findByProductId(Long productId);
    Optional<PriceList> findByProductIdAndBranchId(Long productId, Long branchId);
    List<PriceList> findByBranchIdAndProductIdIn(Long branchId, java.util.Collection<Long> productIds);
}
