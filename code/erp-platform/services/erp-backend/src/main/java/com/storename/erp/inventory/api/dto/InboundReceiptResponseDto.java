package com.storename.erp.inventory.api.dto;

import com.storename.erp.inventory.domain.InboundReceipt;
import com.storename.erp.inventory.domain.InboundReceiptLine;
import com.storename.erp.inventory.domain.ReceiptStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class InboundReceiptResponseDto {
    private UUID id;
    private Long branchId;
    private String receiptCode;
    private ReceiptStatus status;
    private UUID supplierId;
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
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private String unitOfMeasure;

        public static LineDto fromEntity(InboundReceiptLine entity) {
            LineDto dto = new LineDto();
            dto.setId(entity.getId());
            dto.setProductId(entity.getProductId());
            dto.setQuantity(entity.getQuantity());
            dto.setUnitCost(entity.getUnitCost());
            dto.setUnitOfMeasure(entity.getUnitOfMeasure());
            return dto;
        }
    }

    public static InboundReceiptResponseDto fromEntity(InboundReceipt entity) {
        InboundReceiptResponseDto dto = new InboundReceiptResponseDto();
        dto.setId(entity.getId());
        dto.setBranchId(entity.getBranchId());
        dto.setReceiptCode(entity.getReceiptCode());
        dto.setStatus(entity.getStatus());
        dto.setSupplierId(entity.getSupplierId());
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
