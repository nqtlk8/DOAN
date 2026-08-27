package com.storename.erp.catalog.api;

import com.storename.erp.catalog.application.ProductReader;
import com.storename.erp.catalog.application.dto.ProductResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.storename.erp.common.api.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/public/catalog")
@RequiredArgsConstructor
public class PublicCatalogController {

    private final ProductReader productReader;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Public REST request to get products");
        List<ProductResponseDto> products = productReader.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProductById(@PathVariable Long id) {
        log.info("Public REST request to get product: {}", id);
        ProductResponseDto product = productReader.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }
}
