package com.storename.erp.catalog.application.dto;

import lombok.Data;

@Data
public class SupplierUpdateDto {
    private String code;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String taxCode;
    private Boolean isActive;
}
