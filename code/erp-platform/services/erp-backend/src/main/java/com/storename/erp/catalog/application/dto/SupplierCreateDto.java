package com.storename.erp.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierCreateDto {
    @NotBlank(message = "Code is required")
    private String code;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String phone;
    private String email;
    private String address;
    private String taxCode;
    private Long branchId;
}
