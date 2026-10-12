package com.storename.erp.order.domain;

import com.storename.erp.common.domain.BaseEntity;
import com.storename.erp.crm.domain.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goods_return")
@Getter
@Setter
public class GoodsReturn extends BaseEntity {

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "return_code", nullable = false, unique = true)
    private String returnCode;

    @Column(name = "invoice_id")
    private UUID invoiceId; // nullable

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoodsReturnStatus status = GoodsReturnStatus.DRAFT;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * Phần tiền trả hàng hoàn lại cho khách vì công nợ không đủ để trừ (khách đã trả trước).
     * totalAmount = phần đã trừ vào công nợ + refundAmount.
     */
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "reason")
    private String reason;

    @Column(name = "note")
    private String note;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @OneToMany(mappedBy = "goodsReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoodsReturnLine> lines = new ArrayList<>();

    public void addLine(GoodsReturnLine line) {
        lines.add(line);
        line.setGoodsReturn(this);
    }

    public void calculateTotal() {
        this.totalAmount = lines.stream()
                .map(l -> l.getUnitPrice().multiply(l.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void confirm(UUID userId) {
        if (this.status != GoodsReturnStatus.DRAFT) {
            throw new com.storename.erp.common.exception.BusinessConflictException(SalesInvoice.INVALID_STATUS,
                    "Phiếu trả " + returnCode + " đã được xác nhận trước đó");
        }
        this.status = GoodsReturnStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.confirmedBy = userId;
    }
}
