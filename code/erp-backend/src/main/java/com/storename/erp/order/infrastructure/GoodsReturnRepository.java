package com.storename.erp.order.infrastructure;

import com.storename.erp.order.domain.GoodsReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoodsReturnRepository extends JpaRepository<GoodsReturn, UUID> {
    Optional<GoodsReturn> findByReturnCode(String returnCode);
    boolean existsByReturnCode(String returnCode);
}
