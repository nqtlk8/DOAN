package com.storename.erp.procurement.domain;

import com.storename.erp.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payable_debt", uniqueConstraints = {
        @UniqueConstraint(name = "uk_debt_supplier_branch", columnNames = {"supplier_id", "branch_id"})
})
@Getter
@Setter
public class PayableDebt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "total_debt", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDebt = BigDecimal.ZERO;

    public void increaseDebt(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.totalDebt = this.totalDebt.add(amount);
    }

    public void decreaseDebt(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        this.totalDebt = this.totalDebt.subtract(amount);
    }
}
