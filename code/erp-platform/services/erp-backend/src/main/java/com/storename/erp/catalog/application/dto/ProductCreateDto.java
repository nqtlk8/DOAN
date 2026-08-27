package com.storename.erp.catalog.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateDto {
    @NotBlank(message = "Product code is required")
    private String code;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Base unit is required")
    private String baseUnit;

    private Boolean isActive;
    private Map<String, Object> attributes;
}
