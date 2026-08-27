package com.storename.erp.analytics.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_alert_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long productId;
    private Long branchId;
    private BigDecimal minQuantityThreshold;
    private String emailRecipients;
    
    @Builder.Default
    private Boolean isActive = true;
    
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
