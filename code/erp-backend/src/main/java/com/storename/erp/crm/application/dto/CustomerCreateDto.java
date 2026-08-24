package com.storename.erp.crm.application.dto;

import com.storename.erp.crm.domain.CustomerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerCreateDto {
    @NotBlank(message = "Customer code is required")
    private String customerCode;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String phone;
    private String email;
    private String address;
    private String taxCode;
    
    @NotNull(message = "Customer type is required")
    private CustomerType customerType;
}
