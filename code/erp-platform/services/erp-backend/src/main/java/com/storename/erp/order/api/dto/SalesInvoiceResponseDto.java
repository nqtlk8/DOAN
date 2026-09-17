package com.storename.erp.order.api.dto;

import com.storename.erp.order.domain.PaymentMethod;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.domain.SalesInvoiceLine;
import com.storename.erp.order.domain.SalesInvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class SalesInvoiceResponseDto {
    private UUID id;
    private Long branchId;
    private UUID customerId;
    private String customerName;
    private String invoiceCode;
    private SalesInvoiceStatus status;
    private BigDecimal totalAmount;
    private BigDecimal previousDebt;
    private BigDecimal remainingDebt;
    private BigDecimal advancePayment;
    private PaymentMethod paymentMethod;
    private String note;
    private LocalDateTime confirmedAt;
    private UUID confirmedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private List<LineDto> lines;

    @Data
    public static class LineDto {
        private UUID id;
        private Long productId;
        private String productName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String unitOfMeasure;

        public static LineDto fromEntity(SalesInvoiceLine entity) {
            LineDto dto = new LineDto();
            dto.setId(entity.getId());
            dto.setProductId(entity.getProductId());
            dto.setProductName(entity.getProductName());
            dto.setQuantity(entity.getQuantity());
            dto.setUnitPrice(entity.getUnitPrice());
            dto.setLineTotal(entity.getLineTotal());
            dto.setUnitOfMeasure(entity.getUnitOfMeasure());
            return dto;
        }
    }

    public static SalesInvoiceResponseDto fromEntity(SalesInvoice entity, String customerName) {
        SalesInvoiceResponseDto dto = new SalesInvoiceResponseDto();
        dto.setId(entity.getId());
        dto.setBranchId(entity.getBranchId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setCustomerName(customerName);
        dto.setInvoiceCode(entity.getInvoiceCode());
        dto.setStatus(entity.getStatus());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setPreviousDebt(entity.getPreviousDebt());
        dto.setRemainingDebt(entity.getRemainingDebt());
        dto.setAdvancePayment(entity.getAdvancePayment());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setNote(entity.getNote());
        dto.setConfirmedAt(entity.getConfirmedAt());
        dto.setConfirmedBy(entity.getConfirmedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        if (entity.getLines() != null) {
            dto.setLines(entity.getLines().stream()
                    .map(LineDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
