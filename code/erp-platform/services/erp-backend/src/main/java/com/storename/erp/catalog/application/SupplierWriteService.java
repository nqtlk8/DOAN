package com.storename.erp.catalog.application;

import com.storename.erp.catalog.application.dto.SupplierCreateDto;
import com.storename.erp.catalog.application.dto.SupplierResponseDto;
import com.storename.erp.catalog.application.dto.SupplierUpdateDto;
import com.storename.erp.catalog.domain.Supplier;
import com.storename.erp.catalog.infrastructure.SupplierRepository;
import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ghi nhà cung cấp (chỉ HQ). Mã NCC và mã số thuế (nếu có) không được trùng với NCC khác,
 * kể cả NCC đã ngừng hoạt động -> HTTP 409.
 */
@Slf4j
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'HQ' or '${instance.role:ALL}' == 'ALL'")
@RequiredArgsConstructor
public class SupplierWriteService {

    private final SupplierRepository supplierRepository;

    @Transactional
    public SupplierResponseDto createSupplier(SupplierCreateDto dto) {
        log.info("Creating supplier: {}", dto.getCode());
        if (supplierRepository.existsByCode(dto.getCode())) {
            throw codeExists(dto.getCode());
        }
        if (hasText(dto.getTaxCode()) && supplierRepository.existsByTaxCode(dto.getTaxCode())) {
            throw taxCodeExists(dto.getTaxCode());
        }
        Supplier supplier = new Supplier();
        supplier.setCode(dto.getCode());
        supplier.setName(dto.getName());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setAddress(dto.getAddress());
        supplier.setTaxCode(dto.getTaxCode());

        Supplier saved = supplierRepository.save(supplier);
        return mapToResponse(saved);
    }

    @Transactional
    public SupplierResponseDto updateSupplier(UUID id, SupplierUpdateDto dto) {
        log.info("Updating supplier: {}", id);
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        if (dto.getCode() != null && !dto.getCode().equals(supplier.getCode())
                && supplierRepository.existsByCodeAndIdNot(dto.getCode(), id)) {
            throw codeExists(dto.getCode());
        }
        if (hasText(dto.getTaxCode()) && !dto.getTaxCode().equals(supplier.getTaxCode())
                && supplierRepository.existsByTaxCodeAndIdNot(dto.getTaxCode(), id)) {
            throw taxCodeExists(dto.getTaxCode());
        }

        if (dto.getCode() != null) supplier.setCode(dto.getCode());
        if (dto.getName() != null) supplier.setName(dto.getName());
        if (dto.getPhone() != null) supplier.setPhone(dto.getPhone());
        if (dto.getEmail() != null) supplier.setEmail(dto.getEmail());
        if (dto.getAddress() != null) supplier.setAddress(dto.getAddress());
        if (dto.getTaxCode() != null) supplier.setTaxCode(dto.getTaxCode());
        if (dto.getIsActive() != null) supplier.setIsActive(dto.getIsActive());

        Supplier saved = supplierRepository.save(supplier);
        return mapToResponse(saved);
    }

    @Transactional
    public void softDeleteSupplier(UUID id) {
        log.info("Soft-deleting supplier: {}", id);
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        supplier.setIsActive(false);
        supplierRepository.save(supplier);
    }

    public SupplierResponseDto mapToResponse(Supplier supplier) {
        return SupplierResponseDto.builder()
                .id(supplier.getId())
                .code(supplier.getCode())
                .name(supplier.getName())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .taxCode(supplier.getTaxCode())
                .isActive(supplier.getIsActive())
                .build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static BusinessConflictException codeExists(String code) {
        return new BusinessConflictException("SUPPLIER_CODE_EXISTS", "Mã nhà cung cấp '" + code + "' đã tồn tại");
    }

    private static BusinessConflictException taxCodeExists(String taxCode) {
        return new BusinessConflictException("SUPPLIER_TAX_CODE_EXISTS", "Mã số thuế '" + taxCode + "' đã thuộc về nhà cung cấp khác");
    }
}
