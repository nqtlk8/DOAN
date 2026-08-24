package com.storename.erp.catalog.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long id;
    private String code;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String baseUnit;
    private boolean isActive;
    private Map<String, Object> attributes;
    private BigDecimal price; // Might be null if not branch scoped or no price list found

    public ProductResponseDto withPrice(BigDecimal price) {
        return new ProductResponseDto(this.id, this.code, this.name, this.categoryId, this.categoryName, this.baseUnit, this.isActive, this.attributes, price);
    }
}
