package com.storename.erp.catalog.api;

import com.storename.erp.catalog.application.ProductWriter;
import com.storename.erp.catalog.application.dto.ProductCreateDto;
import com.storename.erp.catalog.application.dto.ProductResponseDto;
import com.storename.erp.catalog.application.dto.ProductUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.storename.erp.common.api.ApiResponse;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/catalog/products")
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'HQ' or '${instance.role:ALL}' == 'ALL'")
@RequiredArgsConstructor
public class ProductWriteController {

    private final ProductWriter productWriter;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(@Valid @RequestBody ProductCreateDto dto) {
        log.info("REST request to create product: {}", dto.getCode());
        ProductResponseDto response = productWriter.createProduct(dto);
        return new ResponseEntity<>(ApiResponse.success(response, "Product created successfully"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateDto dto) {
        log.info("REST request to update product: {}", id);
        ProductResponseDto response = productWriter.updateProduct(id, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("REST request to soft-delete product: {}", id);
        productWriter.softDeleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }
}
