package com.storename.erp.crm.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDto {
    private UUID id;
    private String customerCode;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String taxCode;
    private Long branchId;
    private boolean isDeleted;
    private LocalDateTime createdAt;

    public static CustomerResponseDto fromEntity(com.storename.erp.crm.domain.Customer customer) {
        if (customer == null) return null;
        return CustomerResponseDto.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .taxCode(customer.getTaxCode())
                .branchId(customer.getBranchId())
                .isDeleted(customer.isDeleted())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
