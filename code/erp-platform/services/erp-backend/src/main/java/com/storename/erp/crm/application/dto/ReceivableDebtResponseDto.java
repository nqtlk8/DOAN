package com.storename.erp.crm.application.dto;

import com.storename.erp.crm.domain.ReceivableDebt;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ReceivableDebtResponseDto {
    private UUID id;
    private UUID customerId;
    private String customerCode;
    private String customerName;
    private String phone;
    private String address;
    private Long branchId;
    private BigDecimal totalDebt;

    public static ReceivableDebtResponseDto fromEntity(ReceivableDebt entity) {
        if (entity == null) return null;
        return ReceivableDebtResponseDto.builder()
                .id(entity.getId())
                .customerId(entity.getCustomer() != null ? entity.getCustomer().getId() : null)
                .customerCode(entity.getCustomer() != null ? entity.getCustomer().getCustomerCode() : null)
                .customerName(entity.getCustomer() != null ? entity.getCustomer().getName() : null)
                .phone(entity.getCustomer() != null ? entity.getCustomer().getPhone() : null)
                .address(entity.getCustomer() != null ? entity.getCustomer().getAddress() : null)
                .branchId(entity.getBranchId())
                .totalDebt(entity.getTotalDebt())
                .build();
    }
}
