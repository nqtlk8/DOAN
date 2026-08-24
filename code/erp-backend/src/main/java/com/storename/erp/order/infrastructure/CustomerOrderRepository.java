package com.storename.erp.order.infrastructure;

import com.storename.erp.order.domain.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
    Optional<CustomerOrder> findByOrderCode(String orderCode);
    boolean existsByOrderCode(String orderCode);
}
