package com.storename.erp.catalog.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateDto {
    private String name;
    private Long categoryId;
    private String baseUnit;
    private Boolean isActive;
    private Map<String, Object> attributes;
}
