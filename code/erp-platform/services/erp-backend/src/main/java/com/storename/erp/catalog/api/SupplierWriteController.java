package com.storename.erp.catalog.api;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.catalog.application.SupplierWriteService;
import com.storename.erp.catalog.application.dto.SupplierCreateDto;
import com.storename.erp.catalog.application.dto.SupplierResponseDto;
import com.storename.erp.catalog.application.dto.SupplierUpdateDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")
public class SupplierWriteController {
    
    private final SupplierWriteService supplierWriteService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<SupplierResponseDto> createSupplier(@Valid @RequestBody SupplierCreateDto dto) {
        log.info("REST request to create supplier: {}", dto.getCode());
        SupplierResponseDto response = supplierWriteService.createSupplier(dto);
        return ApiResponse.success(response, "Supplier created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<SupplierResponseDto> updateSupplier(
            @PathVariable UUID id, 
            @Valid @RequestBody SupplierUpdateDto dto) {
        log.info("REST request to update supplier: {}", id);
        SupplierResponseDto response = supplierWriteService.updateSupplier(id, dto);
        return ApiResponse.success(response, "Supplier updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<Void> deleteSupplier(@PathVariable UUID id) {
        log.info("REST request to soft-delete supplier: {}", id);
        supplierWriteService.softDeleteSupplier(id);
        return ApiResponse.success(null, "Supplier deleted successfully");
    }
}
