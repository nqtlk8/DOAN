package com.storename.erp.catalog.api;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.catalog.application.dto.SupplierResponseDto;
import com.storename.erp.catalog.domain.Supplier;
import com.storename.erp.catalog.infrastructure.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierReadController {
    
    private final SupplierRepository supplierRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<SupplierResponseDto>> getSuppliers() {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchId();
        log.info("REST request to get suppliers, branchId: {}", branchId);
        List<Supplier> suppliers;
        if (branchId != null) {
            suppliers = supplierRepository.findByBranchId(branchId);
        } else {
            suppliers = supplierRepository.findAll();
        }
        
        List<SupplierResponseDto> response = suppliers.stream()
                .map(this::mapToResponse)
                .toList();
                
        log.debug("Returning {} suppliers", response.size());
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<SupplierResponseDto> getSupplier(@PathVariable UUID id) {
        log.info("REST request to get supplier: {}", id);
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
                
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchId();
        if (branchId != null && !branchId.equals(supplier.getBranchId())) {
             throw new org.springframework.security.access.AccessDeniedException("Unauthorized");
        }
        return ApiResponse.success(mapToResponse(supplier));
    }
    

    
    private SupplierResponseDto mapToResponse(Supplier supplier) {
        return SupplierResponseDto.builder()
                .id(supplier.getId())
                .code(supplier.getCode())
                .name(supplier.getName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .taxCode(supplier.getTaxCode())
                .isActive(supplier.getIsActive() != null ? supplier.getIsActive() : false)
                .branchId(supplier.getBranchId())
                .build();
    }
}

