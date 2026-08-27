package com.storename.erp.catalog.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "product")
@Getter

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "base_unit", nullable = false, length = 50)
    private String baseUnit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes_cache")
    private Map<String, Object> attributesCache;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // --- Domain Behavior Methods ---

    public void updateDetails(Category category, String name, String baseUnit) {
        if (category != null) this.category = category;
        if (name != null && !name.trim().isEmpty()) this.name = name;
        if (baseUnit != null && !baseUnit.trim().isEmpty()) this.baseUnit = baseUnit;
    }

    public void changeActiveState(boolean active) {
        this.isActive = active;
    }

    public void updateAttributesCache(Map<String, Object> newCache) {
        this.attributesCache = newCache;
    }
}
