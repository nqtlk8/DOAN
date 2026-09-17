package com.storename.erp.crm.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "receivable_debt_movement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceivableDebtMovement {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private ReceivableDebtMovementType movementType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "ref_type", nullable = false, length = 50)
    private String refType;

    @Column(name = "ref_id", nullable = false, length = 100)
    private String refId;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    public static ReceivableDebtMovement create(
            Long branchId,
            Customer customer,
            ReceivableDebtMovementType movementType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String refType,
            String refId,
            UUID createdBy,
            String note,
            String idempotencyKey) {
        
        if (balanceBefore.add(amount).compareTo(balanceAfter) != 0) {
            throw new IllegalArgumentException("Invariant violation: balanceBefore + amount != balanceAfter");
        }

        ReceivableDebtMovement m = new ReceivableDebtMovement();
        m.branchId = branchId;
        m.customer = customer;
        m.movementType = movementType;
        m.amount = amount;
        m.balanceBefore = balanceBefore;
        m.balanceAfter = balanceAfter;
        m.refType = refType;
        m.refId = refId;
        m.createdBy = createdBy;
        m.note = note;
        m.idempotencyKey = idempotencyKey;
        return m;
    }
}
