package com.storename.erp.branch.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDTO {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String phone;
    private String openingHours;
    private String internalUrl;
    /**
     * Tên JSON là {@code isActive}. Kiểu Boolean để phân biệt "không gửi" (null, giữ nguyên khi cập nhật)
     * với {@code false}.
     */
    @JsonProperty("isActive")
    private Boolean isActive;
}
