package com.storename.erp.procurement.infrastructure;

import com.storename.erp.procurement.domain.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID> {
    Optional<StockTransfer> findByTransferCode(String transferCode);
    boolean existsByTransferCode(String transferCode);
}
