package com.storename.erp.catalog.api;

import com.storename.erp.catalog.application.ProductReader;
import com.storename.erp.catalog.application.dto.ProductResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.storename.erp.common.api.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
public class ProductReadController {

    private final ProductReader productReader;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAllProducts(
            @RequestParam(required = false, defaultValue = "false") boolean withBranchPrice) {
        log.info("REST request to get all products");
        List<ProductResponseDto> products;
        if (withBranchPrice) {
            products = productReader.getAllProductsWithBranchPrice();
        } else {
            products = productReader.getAllProducts();
        }
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean withBranchPrice) {
        log.info("REST request to get product: {}", id);
        ProductResponseDto product;
        if (withBranchPrice) {
            product = productReader.getProductByIdWithBranchPrice(id);
        } else {
            product = productReader.getProductById(id);
        }
        return ResponseEntity.ok(ApiResponse.success(product));
    }
}
