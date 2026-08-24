package com.storename.erp.inventory.application.dto;

import com.storename.erp.inventory.domain.ReceiptStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OutboundReceiptResponseDto {
    private UUID id;
    private UUID branchId;
    private String receiptCode;
    private ReceiptStatus status;
    private String reason;
    private String note;
    private LocalDateTime confirmedAt;
    private UUID confirmedBy;
    private List<LineDto> lines;

    @Data
    public static class LineDto {
        private UUID id;
        private UUID productId;
        private BigDecimal quantity;
        private String unitOfMeasure;
    }
}
