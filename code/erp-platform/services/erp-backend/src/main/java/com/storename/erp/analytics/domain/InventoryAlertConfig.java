package com.storename.erp.analytics.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Configuration for inventory alerts. Replicated to branches if created at HQ.
 */
@Entity
@Table(
    name = "inventory_alert_config",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_alert_config_product_branch", columnNames = {"product_id", "branch_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "branch_id", nullable = false)
    private Long branchId;
    
    @Column(name = "min_quantity_threshold", nullable = false)
    private BigDecimal minQuantityThreshold;
    
    @Column(name = "email_recipients")
    private String emailRecipients;
    
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
