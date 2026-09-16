package com.storename.erp.catalog.application;

import com.storename.erp.catalog.application.dto.ProductResponseDto;
import com.storename.erp.catalog.domain.Product;
import com.storename.erp.catalog.domain.PriceList;
import com.storename.erp.catalog.infrastructure.PriceListRepository;
import com.storename.erp.catalog.infrastructure.ProductRepository;
import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.common.aop.BranchScoped;
import com.storename.erp.common.security.JwtAuthDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReader {

    private final ProductRepository productRepository;
    private final PriceListRepository priceListRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "products")
    public List<ProductResponseDto> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public ProductResponseDto getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProductsWithBranchPrice() {
        log.info("Fetching all products with branch price");
        Long branchId = getCurrentBranchId();
        
        return productRepository.findAll().stream()
                .map(product -> mapToResponseWithPrice(product, branchId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductByIdWithBranchPrice(Long id) {
        log.info("Fetching product with id: {} and branch price", id);
        Long branchId = getCurrentBranchId();
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return mapToResponseWithPrice(product, branchId);
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

    private ProductResponseDto mapToResponseWithPrice(Product product, Long branchId) {
        ProductResponseDto dto = mapToResponse(product);
        
        priceListRepository.findByProductIdAndBranchId(product.getId(), branchId)
                .ifPresent(priceList -> dto.setPrice(priceList.getPrice()));
                
        return dto;
    }

    private Long getCurrentBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails) {
            String branchIdStr = ((JwtAuthDetails) auth.getDetails()).getBranchId();
            if (branchIdStr != null && !branchIdStr.trim().isEmpty()) {
                try {
                    return Long.parseLong(branchIdStr);
                } catch (NumberFormatException e) {
                    log.error("Invalid branch ID format: {}", branchIdStr);
                }
            }
        }
        throw new SecurityException("Branch ID is required but missing from security context");
    }
}

