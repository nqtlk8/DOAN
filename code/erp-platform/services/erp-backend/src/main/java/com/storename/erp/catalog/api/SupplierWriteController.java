package com.storename.erp.catalog.api;

import com.storename.erp.catalog.domain.Supplier;
import com.storename.erp.catalog.infrastructure.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")
public class SupplierWriteController {
    private final SupplierRepository supplierRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public Supplier createSupplier(@RequestBody Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Supplier updateSupplier(@PathVariable java.util.UUID id, @RequestBody Supplier payload) {
        Supplier supplier = supplierRepository.findById(id).orElseThrow(() -> new RuntimeException("Supplier not found"));
        supplier.setName(payload.getName());
        supplier.setCode(payload.getCode());
        supplier.setPhone(payload.getPhone());
        supplier.setEmail(payload.getEmail());
        supplier.setAddress(payload.getAddress());
        supplier.setTaxCode(payload.getTaxCode());
        supplier.setIsActive(payload.getIsActive());
        return supplierRepository.save(supplier);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteSupplier(@PathVariable java.util.UUID id) {
        supplierRepository.deleteById(id);
    }
}
