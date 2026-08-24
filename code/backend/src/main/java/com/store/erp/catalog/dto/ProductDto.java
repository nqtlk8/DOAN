package com.store.erp.catalog.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProductDto {
    private UUID id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal cost;
    private String unit;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

