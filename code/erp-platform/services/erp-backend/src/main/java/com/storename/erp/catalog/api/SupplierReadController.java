package com.storename.erp.catalog.api;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.common.security.AuthUtils;
import com.storename.erp.catalog.application.dto.SupplierResponseDto;
import com.storename.erp.catalog.domain.Supplier;
import com.storename.erp.catalog.infrastructure.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API đọc nhà cung cấp. Chạy ở cả HQ và Branch (Branch đọc bản replicate từ HQ).
 *
 * <p>Supplier là dữ liệu dùng chung: người dùng chi nhánh (JWT có branchId) thấy mọi supplier
 * đang hoạt động; ADMIN tại HQ (không có branchId) thấy toàn bộ, kể cả đã ngừng hoạt động.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierReadController {

    private final SupplierRepository supplierRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<SupplierResponseDto>> getSuppliers() {
        boolean branchUser = AuthUtils.getBranchIdOrNull() != null;
        log.info("REST request to get suppliers, branchUser: {}", branchUser);
        List<Supplier> suppliers = branchUser
                ? supplierRepository.findByIsActiveTrueOrderByNameAsc()
                : supplierRepository.findAllByOrderByNameAsc();

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
        // Không kiểm tra chi nhánh: phiếu nhập cũ có thể tham chiếu supplier đã ngừng hoạt động.
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
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
                .build();
    }
}
