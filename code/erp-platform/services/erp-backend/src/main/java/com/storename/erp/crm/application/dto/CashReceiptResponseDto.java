package com.storename.erp.crm.application.dto;

import com.storename.erp.crm.domain.CashReceipt;
import com.storename.erp.crm.domain.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Có constructor rỗng để Jackson đọc lại được: {@code IdempotencyAspect} lưu phản hồi của API hủy phiếu thu
 * dưới dạng JSON và trả lại đúng phản hồi đó khi client gửi lại cùng Idempotency-Key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashReceiptResponseDto {
    private UUID id;
    private String receiptCode;
    private Long branchId;
    private UUID customerId;
    private String customerCode;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private LocalDate receiptDate;
    private BigDecimal amount;
    private BigDecimal debtBefore;
    private BigDecimal debtAfter;
    private String note;
    private String status;
    private LocalDateTime createdAt;
    private UUID createdBy;
    private LocalDateTime cancelledAt;
    private UUID cancelledBy;
    private String cancelReason;

    public static CashReceiptResponseDto from(CashReceipt r, Customer c) {
        return CashReceiptResponseDto.builder()
                .id(r.getId())
                .receiptCode(r.getReceiptCode())
                .branchId(r.getBranchId())
                .customerId(r.getCustomerId())
                .customerCode(c != null ? c.getCustomerCode() : null)
                .customerName(c != null ? c.getName() : null)
                .customerPhone(c != null ? c.getPhone() : null)
                .customerAddress(c != null ? c.getAddress() : null)
                .receiptDate(r.getReceiptDate())
                .amount(r.getAmount())
                .debtBefore(r.getDebtBefore())
                .debtAfter(r.getDebtAfter())
                .note(r.getNote())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .createdAt(r.getCreatedAt())
                .createdBy(r.getCreatedBy())
                .cancelledAt(r.getCancelledAt())
                .cancelledBy(r.getCancelledBy())
                .cancelReason(r.getCancelReason())
                .build();
    }
}
