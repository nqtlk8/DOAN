package com.store.erp.catalog.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DistributorDto {
    private UUID id;
    private String code;
    private String name;
    private String contactName;
    private String phone;
    private String email;
    private String address;
    private String region;
    private String taxCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

