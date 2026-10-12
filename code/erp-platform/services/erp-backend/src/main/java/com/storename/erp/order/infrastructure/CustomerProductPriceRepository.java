package com.storename.erp.order.infrastructure;

import com.storename.erp.order.domain.CustomerProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerProductPriceRepository extends JpaRepository<CustomerProductPrice, UUID> {
    Optional<CustomerProductPrice> findByCustomerIdAndProductIdAndBranchId(UUID customerId, Long productId, Long branchId);

    /** Mọi giá riêng (kể cả đã hết hạn) của khách tại chi nhánh cho các sản phẩm — lọc hiệu lực ở service. */
    List<CustomerProductPrice> findByCustomerIdAndBranchIdAndProductIdIn(UUID customerId, Long branchId, Collection<Long> productIds);
}
