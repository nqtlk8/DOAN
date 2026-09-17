package com.storename.erp.order.api.dto;

import com.storename.erp.order.domain.GoodsReturn;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class GoodsReturnResponseDto {
    private UUID id;
    private String returnCode;
    private Long branchId;
    private UUID customerId;
    private String customerName; // We'll look this up separately if needed
    private UUID invoiceId;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private String reason;
    private java.time.LocalDateTime createdAt;
    private UUID createdBy;

    private List<GoodsReturnLineResponseDto> lines;

    @Data
    @Builder
    public static class GoodsReturnLineResponseDto {
        private UUID id;
        private Long productId;
        private String productCode; // Needs lookup
        private String productName; // Needs lookup
        private Integer quantity;
        private String unitOfMeasure;
        private BigDecimal unitPrice;
        private BigDecimal lineAmount;
    }

    public static GoodsReturnResponseDto fromEntity(GoodsReturn entity, String customerName) {
        if (entity == null) return null;

        List<GoodsReturnLineResponseDto> lineDtos = null;
        if (entity.getLines() != null) {
            lineDtos = entity.getLines().stream()
                    .map(line -> GoodsReturnLineResponseDto.builder()
                            .id(line.getId())
                            .productId(line.getProductId())
                            .quantity(line.getQuantity().intValue())
                            .unitOfMeasure(line.getUnitOfMeasure())
                            .unitPrice(line.getUnitPrice())
                            .lineAmount(line.getUnitPrice().multiply(line.getQuantity()))
                            .build())
                    .collect(Collectors.toList());
        }

        return GoodsReturnResponseDto.builder()
                .id(entity.getId())
                .returnCode(entity.getReturnCode())
                .branchId(entity.getBranchId())
                .customerId(entity.getCustomerId() != null ? entity.getCustomerId() : null)
                .customerName(customerName)
                .invoiceId(entity.getInvoiceId())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .totalAmount(entity.getTotalAmount())
                .refundAmount(entity.getTotalAmount()) // Refund amount is technically totalAmount in this context
                .reason(entity.getReason())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .lines(lineDtos)
                .build();
    }
}
