package com.store.erp.catalog.controller;

import com.store.erp.core.dto.ApiResponse;
import com.store.erp.catalog.dto.CustomerDto;
import com.store.erp.core.dto.PageDto;
import com.store.erp.catalog.dto.ProductDto;
import com.store.erp.catalog.dto.DistributorDto;
import com.store.erp.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService CatalogService;

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<PageDto<CustomerDto>>> getCustomers(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageDto<CustomerDto> result = CatalogService.getCustomers(name, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, result, "Customers retrieved successfully", null));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PageDto<ProductDto>>> getProducts(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageDto<ProductDto> result = CatalogService.getProducts(code, name, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, result, "Products retrieved successfully", null));
    }

    @GetMapping("/distributors")
    public ResponseEntity<ApiResponse<PageDto<DistributorDto>>> getDistributors(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageDto<DistributorDto> result = CatalogService.getDistributors(search, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, result, "Distributors retrieved successfully", null));
    }

    @PostMapping("/customers")
    public ResponseEntity<ApiResponse<CustomerDto>> createCustomer(@RequestBody CustomerDto request) {
        CustomerDto result = CatalogService.createCustomer(request);
        return ResponseEntity.status(201).body(new ApiResponse<>(true, result, "Customer created successfully", null));
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> updateCustomer(@PathVariable UUID id, @RequestBody CustomerDto request) {
        CustomerDto result = CatalogService.updateCustomer(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, result, "Customer updated successfully", null));
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable UUID id) {
        CatalogService.deleteCustomer(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Customer deleted successfully", null));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(@RequestBody ProductDto request) {
        ProductDto result = CatalogService.createProduct(request);
        return ResponseEntity.status(201).body(new ApiResponse<>(true, result, "Product created successfully", null));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(@PathVariable UUID id, @RequestBody ProductDto request) {
        ProductDto result = CatalogService.updateProduct(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, result, "Product updated successfully", null));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        CatalogService.deleteProduct(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Product deleted successfully", null));
    }

    @PostMapping("/distributors")
    public ResponseEntity<ApiResponse<DistributorDto>> createDistributor(@RequestBody DistributorDto request) {
        DistributorDto result = CatalogService.createDistributor(request);
        return ResponseEntity.status(201).body(new ApiResponse<>(true, result, "Distributor created successfully", null));
    }

    @PutMapping("/distributors/{id}")
    public ResponseEntity<ApiResponse<DistributorDto>> updateDistributor(@PathVariable UUID id, @RequestBody DistributorDto request) {
        DistributorDto result = CatalogService.updateDistributor(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, result, "Distributor updated successfully", null));
    }

    @DeleteMapping("/distributors/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDistributor(@PathVariable UUID id) {
        CatalogService.deleteDistributor(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Distributor deleted successfully", null));
    }
}

