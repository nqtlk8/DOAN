package com.storename.erp.catalog.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "price_list", uniqueConstraints = @UniqueConstraint(
        name = "uk_price_list_product_branch", columnNames = {"product_id", "branch_id"}))
@Getter

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Use branchId directly instead of cross-domain entity reference
    @Column(name = "branch_id", nullable = false)
    private Long branchId;


    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "effective_date", nullable = false)
    private OffsetDateTime effectiveDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
