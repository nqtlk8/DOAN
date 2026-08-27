package com.storename.erp.analytics.infrastructure;

import com.storename.erp.analytics.domain.InventoryAlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryAlertConfigRepository extends JpaRepository<InventoryAlertConfig, Long> {
    List<InventoryAlertConfig> findByIsActiveTrue();
}
