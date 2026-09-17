package com.storename.erp.order.domain;

import com.storename.erp.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sales_invoice")
@Getter
@Setter
public class SalesInvoice extends BaseEntity {

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "invoice_code", nullable = false, unique = true, length = 100)
    private String invoiceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SalesInvoiceStatus status = SalesInvoiceStatus.DRAFT;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "previous_debt", nullable = false, precision = 19, scale = 4)
    private BigDecimal previousDebt = BigDecimal.ZERO;

    @Column(name = "remaining_debt", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingDebt = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "advance_payment", precision = 19, scale = 4)
    private BigDecimal advancePayment = BigDecimal.ZERO;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesInvoiceLine> lines = new ArrayList<>();

    public void addLine(SalesInvoiceLine line) {
        lines.add(line);
        line.setInvoice(this);
    }

    public void confirm(UUID userId) {
        if (this.status != SalesInvoiceStatus.DRAFT)
            throw new IllegalStateException("Only DRAFT can be confirmed");
        this.status = SalesInvoiceStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.confirmedBy = userId;
    }

    public void snapshotDebt(BigDecimal previousDebt, BigDecimal remainingDebt) {
        this.previousDebt = previousDebt;
        this.remainingDebt = remainingDebt;
    }

    public void calculateTotal() {
        this.totalAmount = lines.stream()
                .map(SalesInvoiceLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
