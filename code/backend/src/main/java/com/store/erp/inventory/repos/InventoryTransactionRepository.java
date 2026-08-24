package com.store.erp.inventory.repos;

import com.store.erp.inventory.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {
    
    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = 'IN' THEN t.quantity ELSE -t.quantity END), 0) FROM InventoryTransaction t WHERE t.product.id = :productId")
    Integer getStockQuantityByProductId(@Param("productId") UUID productId);

    boolean existsByProductId(UUID productId);
}

