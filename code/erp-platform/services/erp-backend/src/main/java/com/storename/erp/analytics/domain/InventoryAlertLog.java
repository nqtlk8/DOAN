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

import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_alert_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAlertLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long productId;
    private Long branchId;
    
    @Builder.Default
    private OffsetDateTime alertedAt = OffsetDateTime.now();
    
    private OffsetDateTime resolvedAt;
    
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, RESOLVED
}
