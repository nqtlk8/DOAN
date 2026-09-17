package com.storename.erp.crm.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerCreateDto {
    private String customerCode;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String phone;
    private String email;
    private String address;
    private String taxCode;
    private Long branchId;
}
