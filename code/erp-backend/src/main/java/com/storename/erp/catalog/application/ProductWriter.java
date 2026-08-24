package com.storename.erp.catalog.application;

import com.storename.erp.catalog.application.dto.ProductCreateDto;
import com.storename.erp.catalog.application.dto.ProductResponseDto;
import com.storename.erp.catalog.application.dto.ProductUpdateDto;
import com.storename.erp.catalog.domain.AttributeDefinition;
import com.storename.erp.catalog.domain.Category;
import com.storename.erp.catalog.domain.Product;
import com.storename.erp.catalog.domain.ProductAttributeValue;
import com.storename.erp.catalog.infrastructure.AttributeDefinitionRepository;
import com.storename.erp.catalog.infrastructure.CategoryRepository;
import com.storename.erp.catalog.infrastructure.ProductAttributeValueRepository;
import com.storename.erp.catalog.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.storename.erp.common.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;

@Slf4j
@Service
@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")
@RequiredArgsConstructor
public class ProductWriter {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final AttributeDefinitionRepository attributeDefinitionRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    /**
     * Creates a new Product at HQ.
     * Clears the products cache.
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponseDto createProduct(ProductCreateDto dto) {
        log.info("Creating new product with code: {}", dto.getCode());

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        boolean isActive = dto.getIsActive() != null ? dto.getIsActive() : true;

        Product product = Product.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .category(category)
                .baseUnit(dto.getBaseUnit())
                .isActive(isActive)
                .attributesCache(new HashMap<>())
                .build();

        product = productRepository.save(product);

        if (dto.getAttributes() != null && !dto.getAttributes().isEmpty()) {
            updateAttributes(product, dto.getAttributes());
            product = productRepository.save(product);
        }

        return mapToResponse(product);
    }

    /**
     * Updates an existing Product at HQ.
     * Clears the products cache.
     */
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponseDto updateProduct(Long id, ProductUpdateDto dto) {
        log.info("Updating product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        product.updateDetails(category, dto.getName(), dto.getBaseUnit());

        if (dto.getIsActive() != null) {
            product.changeActiveState(dto.getIsActive());
        }

        if (dto.getAttributes() != null) {
            updateAttributes(product, dto.getAttributes());
        }

        product = productRepository.save(product);

        return mapToResponse(product);
    }

    private void updateAttributes(Product product, Map<String, Object> attributes) {
        Map<String, Object> cache = product.getAttributesCache();
        if (cache == null) {
            cache = new HashMap<>();
        }

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String attrCode = entry.getKey();
            Object value = entry.getValue();

            AttributeDefinition attrDef = attributeDefinitionRepository.findByCode(attrCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Attribute definition not found: " + attrCode));

            ProductAttributeValue.ProductAttributeValueId attrId = new ProductAttributeValue.ProductAttributeValueId(product.getId(), attrDef.getId());
            ProductAttributeValue attrValue = productAttributeValueRepository.findById(attrId)
                    .orElse(ProductAttributeValue.builder()
                            .id(attrId)
                            .product(product)
                            .attribute(attrDef)
                            .build());

            if ("NUMBER".equalsIgnoreCase(attrDef.getDataType())) {
                attrValue.updateValueNumber(new BigDecimal(value.toString()));
                cache.put(attrCode, attrValue.getValueNumber());
            } else if ("BOOLEAN".equalsIgnoreCase(attrDef.getDataType())) {
                attrValue.updateValueBoolean(Boolean.valueOf(value.toString()));
                cache.put(attrCode, attrValue.getValueBoolean());
            } else {
                attrValue.updateValueText(value.toString());
                cache.put(attrCode, attrValue.getValueText());
            }

            productAttributeValueRepository.save(attrValue);
        }
        product.updateAttributesCache(cache);
    }

    private ProductResponseDto mapToResponse(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .baseUnit(product.getBaseUnit())
                .isActive(product.isActive())
                .attributes(product.getAttributesCache())
                .build();
    }
}
