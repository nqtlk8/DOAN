package com.storename.erp.order.api;

import lombok.extern.slf4j.Slf4j;
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
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "instance.role", havingValue = "BRANCH")
@Slf4j
public class SalesInvoiceController {

    private final SalesInvoiceService salesInvoiceService;

    public SalesInvoiceController(SalesInvoiceService salesInvoiceService) {
        this.salesInvoiceService = salesInvoiceService;
    }


    private UUID getUserId() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UUID> createDraft(
            @Valid @RequestBody SalesInvoiceCreateDto dto) {
        
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        SalesInvoice invoice = salesInvoiceService.createDraft(dto, branchId);
        return ApiResponse.success(invoice.getId(), "Sales invoice created successfully");
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('STAFF')")
    @IdempotencyProtected
    public ApiResponse<UUID> confirmInvoice(
            @PathVariable UUID id) {
        
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        UUID userId = getUserId();
        
        SalesInvoice invoice = salesInvoiceService.confirmInvoice(id, userId, branchId);
        return ApiResponse.success(invoice.getId(), "Sales invoice confirmed successfully");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STAFF')")
    public ApiResponse<List<com.storename.erp.order.api.dto.SalesInvoiceResponseDto>> getInvoices() {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        List<com.storename.erp.order.api.dto.SalesInvoiceResponseDto> dtoList = salesInvoiceService.getInvoicesByBranch(branchId)
                .stream().map(com.storename.erp.order.api.dto.SalesInvoiceResponseDto::fromEntity)
                .toList();
        return ApiResponse.success(dtoList);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STAFF')")
    public ApiResponse<com.storename.erp.order.api.dto.SalesInvoiceResponseDto> getInvoice(@PathVariable UUID id) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        SalesInvoice invoice = salesInvoiceService.getInvoice(id, branchId);
        return ApiResponse.success(com.storename.erp.order.api.dto.SalesInvoiceResponseDto.fromEntity(invoice));
    }
}



