package com.storename.erp.catalog.api;

import com.storename.erp.catalog.domain.Supplier;
import com.storename.erp.catalog.infrastructure.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierRepository supplierRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public List<Supplier> getSuppliers(@RequestParam(required = false) Long branchId) {
        if (branchId != null) {
            return supplierRepository.findByBranchId(branchId);
        }
        return supplierRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public Supplier getSupplier(@PathVariable java.util.UUID id) {
        return supplierRepository.findById(id).orElseThrow(() -> new RuntimeException("Supplier not found"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public Supplier createSupplier(@RequestBody Supplier supplier) {
        return supplierRepository.save(supplier);
    }
}
