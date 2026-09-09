package com.storename.erp.crm.application.dto;


import lombok.Data;

@Data
public class CustomerUpdateDto {
    private String name;
    private String phone;
    private String email;
    private String address;
    private String taxCode;
}
