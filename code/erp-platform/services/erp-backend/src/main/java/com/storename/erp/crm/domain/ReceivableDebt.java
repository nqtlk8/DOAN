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

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "total_debt", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDebt = BigDecimal.ZERO;

}
