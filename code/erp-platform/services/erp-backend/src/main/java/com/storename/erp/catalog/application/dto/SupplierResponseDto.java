package com.storename.erp.catalog.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponseDto {
    private UUID id;
    private String code;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String taxCode;
    /** Tên JSON là {@code isActive} (khớp kiểu {@code Supplier} ở frontend), không phải {@code active} mặc định của Jackson. */
    @JsonProperty("isActive")
    private boolean isActive;
}
