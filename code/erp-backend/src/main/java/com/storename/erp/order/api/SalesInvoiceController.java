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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sales-invoices")
@BranchScoped
public class SalesInvoiceController {

    private final SalesInvoiceService salesInvoiceService;

    public SalesInvoiceController(SalesInvoiceService salesInvoiceService) {
        this.salesInvoiceService = salesInvoiceService;
    }

    private UUID getBranchId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails details) {
            return UUID.fromString(details.getBranchId());
        }
        throw new IllegalStateException("Branch ID not found in security context");
    }

    private UUID getUserId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return UUID.nameUUIDFromBytes(auth.getName().getBytes());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SALE_CREATE', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UUID> createDraft(
            @Valid @RequestBody SalesInvoiceCreateDto dto) {
        
        UUID branchId = getBranchId();
        SalesInvoice invoice = salesInvoiceService.createDraft(dto, branchId);
        return ApiResponse.success(invoice.getId(), "Sales invoice created successfully");
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('SALE_CONFIRM', 'ADMIN')")
    @IdempotencyProtected
    public ApiResponse<UUID> confirmInvoice(
            @PathVariable UUID id) {
        
        UUID branchId = getBranchId();
        UUID userId = getUserId();
        
        SalesInvoice invoice = salesInvoiceService.confirmInvoice(id, userId, branchId);
        return ApiResponse.success(invoice.getId(), "Sales invoice confirmed successfully");
    }
}
