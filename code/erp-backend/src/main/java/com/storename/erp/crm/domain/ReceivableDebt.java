package com.storename.erp.crm.domain;

import com.storename.erp.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "receivable_debt", uniqueConstraints = @UniqueConstraint(
        name = "uk_debt_customer_branch",
        columnNames = {"customer_id", "branch_id"}))
@Getter
@Setter
public class ReceivableDebt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "total_debt", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDebt = BigDecimal.ZERO;

    public void increaseDebt(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.totalDebt = this.totalDebt.add(amount);
    }

    public void decreaseDebt(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.totalDebt = this.totalDebt.subtract(amount);
    }
}
