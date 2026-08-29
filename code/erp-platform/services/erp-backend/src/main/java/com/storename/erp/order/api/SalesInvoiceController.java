package com.storename.erp.order.api;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.aop.BranchScoped;
import com.storename.erp.common.aop.IdempotencyProtected;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.order.application.SalesInvoiceService;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.domain.SalesInvoice;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sales-invoices")
@BranchScoped
public class SalesInvoiceController {

    private final SalesInvoiceService salesInvoiceService;

    public SalesInvoiceController(SalesInvoiceService salesInvoiceService) {
        this.salesInvoiceService = salesInvoiceService;
    }

    private Long getBranchId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails details) {
            return Long.parseLong(details.getBranchId());
        }
        throw new IllegalStateException("Branch ID not found in security context");
    }

    private UUID getUserId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UUID> createDraft(
            @Valid @RequestBody SalesInvoiceCreateDto dto) {
        
        Long branchId = getBranchId();
        SalesInvoice invoice = salesInvoiceService.createDraft(dto, branchId);
        return ApiResponse.success(invoice.getId(), "Sales invoice created successfully");
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    @IdempotencyProtected
    public ApiResponse<UUID> confirmInvoice(
            @PathVariable UUID id) {
        
        Long branchId = getBranchId();
        UUID userId = getUserId();
        
        SalesInvoice invoice = salesInvoiceService.confirmInvoice(id, userId, branchId);
        return ApiResponse.success(invoice.getId(), "Sales invoice confirmed successfully");
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<SalesInvoice>> getInvoices() {
        Long branchId = getBranchId();
        return ApiResponse.success(salesInvoiceService.getInvoicesByBranch(branchId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<SalesInvoice> getInvoice(@PathVariable UUID id) {
        Long branchId = getBranchId();
        return ApiResponse.success(salesInvoiceService.getInvoice(id, branchId));
    }
}