package com.storename.erp.crm.application.dto;

import com.storename.erp.crm.domain.ReceivableDebtMovement;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
public class ReceivableDebtMovementResponseDto {
    private UUID id;
    private Long branchId;
    private UUID customerId;
    private String movementType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String refType;
    private String refId;
    private ZonedDateTime createdAt;
    private UUID createdBy;
    private String note;
    
    private String referenceCode;
    private String branchName;

    public static ReceivableDebtMovementResponseDto fromEntity(ReceivableDebtMovement movement) {
        if (movement == null) {
            return null;
        }
        return ReceivableDebtMovementResponseDto.builder()
                .id(movement.getId())
                .branchId(movement.getBranchId())
                .customerId(movement.getCustomer() != null ? movement.getCustomer().getId() : null)
                .movementType(movement.getMovementType() != null ? movement.getMovementType().name() : null)
                .amount(movement.getAmount())
                .balanceAfter(movement.getBalanceAfter())
                .refType(movement.getRefType())
                .refId(movement.getRefId())
                .createdAt(movement.getCreatedAt())
                .createdBy(movement.getCreatedBy())
                .note(movement.getNote())
                .build();
    }
}
