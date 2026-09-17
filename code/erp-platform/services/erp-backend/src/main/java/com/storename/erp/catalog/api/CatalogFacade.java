package com.storename.erp.catalog.api;

import com.storename.erp.catalog.infrastructure.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.Builder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogFacade {
    private final ProductRepository productRepository;

    @Data
    @Builder
    public static class ProductBasicInfo {
        private String code;
        private String name;
    }

    public Map<Long, ProductBasicInfo> getProductBasicInfo(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Map.of();
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(
                        com.storename.erp.catalog.domain.Product::getId,
                        p -> ProductBasicInfo.builder()
                                .code(p.getCode())
                                .name(p.getName())
                                .build()
                ));
    }
}
