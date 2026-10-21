package com.storename.erp.crm.domain;

import com.storename.erp.common.domain.BaseEntity;
import com.storename.erp.common.exception.BusinessConflictException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Phiếu thu tiền khách hàng tại một chi nhánh. Lập phiếu = giảm công nợ (PAYMENT);
 * hủy phiếu = tăng lại công nợ đúng số tiền (ADJUSTMENT). Không có sửa / xóa.
 */
@Entity
@Table(name = "cash_receipt")
@Getter
@Setter
public class CashReceipt extends BaseEntity {

    /** Mã lỗi (HTTP 409): hủy một phiếu thu đã hủy trước đó. */
    public static final String ALREADY_CANCELLED = "RECEIPT_ALREADY_CANCELLED";

    @Column(name = "receipt_code", nullable = false, unique = true, length = 100)
    private String receiptCode;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    /** Khóa lập phiếu (Idempotency-Key kèm phạm vi người dùng); UNIQUE, xem db/migration/V5__sales.sql. */
    @Column(name = "request_key", unique = true, length = 255)
    private String requestKey;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "debt_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal debtBefore;

    @Column(name = "debt_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal debtAfter;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashReceiptStatus status = CashReceiptStatus.POSTED;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    /** Ghi số nợ trước/sau khi thu (bất biến: debtAfter = debtBefore - amount, có CHECK ở DB). */
    public void snapshotDebt(BigDecimal debtBefore, BigDecimal debtAfter) {
        this.debtBefore = debtBefore;
        this.debtAfter = debtAfter;
    }

    public boolean isCancelled() {
        return status == CashReceiptStatus.CANCELLED;
    }

    public void cancel(UUID userId, String reason) {
        if (isCancelled()) {
            throw new BusinessConflictException(ALREADY_CANCELLED, "Phiếu thu " + receiptCode + " đã được hủy trước đó");
        }
        this.status = CashReceiptStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = userId;
        this.cancelReason = reason;
    }
}
