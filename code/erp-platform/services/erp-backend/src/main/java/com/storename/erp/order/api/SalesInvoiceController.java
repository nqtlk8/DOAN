package com.storename.erp.order.api;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.aop.IdempotencyContext;
import com.storename.erp.common.aop.IdempotencyProtected;
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
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'BRANCH' or '${instance.role:ALL}' == 'ALL'")
@Slf4j
public class SalesInvoiceController {

    private final SalesInvoiceService salesInvoiceService;

    public SalesInvoiceController(SalesInvoiceService salesInvoiceService) {
        this.salesInvoiceService = salesInvoiceService;
    }


    private UUID getUserId() {
        return com.storename.erp.common.security.AuthUtils.getUserId();
    }

    /** Bước 1 — Lưu hóa đơn nháp (DRAFT). Chưa trừ kho, chưa ghi công nợ. */
    @PostMapping
    @PreAuthorize("hasAuthority('STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    @IdempotencyProtected
    public ApiResponse<com.storename.erp.order.application.dto.SalesInvoiceCreateResponseDto> createInvoice(
            @Valid @RequestBody SalesInvoiceCreateDto dto) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        SalesInvoice invoice = salesInvoiceService.createDraft(dto, branchId, IdempotencyContext.currentRequestKey());
        return ApiResponse.success(new com.storename.erp.order.application.dto.SalesInvoiceCreateResponseDto(invoice.getId(), invoice.getInvoiceCode()), "Sales invoice draft created successfully");
    }

    /** Sửa hóa đơn khi còn nháp (DRAFT). Hóa đơn đã xác nhận -> 409 INVALID_STATUS. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STAFF')")
    @IdempotencyProtected
    public ApiResponse<com.storename.erp.order.application.dto.SalesInvoiceCreateResponseDto> updateInvoice(
            @PathVariable("id") UUID invoiceId, @Valid @RequestBody SalesInvoiceCreateDto dto) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        SalesInvoice invoice = salesInvoiceService.updateDraft(invoiceId, dto, branchId);
        return ApiResponse.success(new com.storename.erp.order.application.dto.SalesInvoiceCreateResponseDto(invoice.getId(), invoice.getInvoiceCode()), "Sales invoice draft updated successfully");
    }

    /** Bước 2 — Xác nhận hóa đơn: trừ kho (FIFO), ghi công nợ, lưu giá riêng của khách. */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('STAFF')")
    @IdempotencyProtected
    public ApiResponse<Void> confirmInvoice(@PathVariable("id") UUID invoiceId) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        salesInvoiceService.confirmInvoice(invoiceId, branchId, getUserId());
        return new ApiResponse<>(true, null, "Sales invoice confirmed successfully", null);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STAFF')")
    public ApiResponse<List<com.storename.erp.order.api.dto.SalesInvoiceResponseDto>> getInvoices() {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        List<com.storename.erp.order.api.dto.SalesInvoiceResponseDto> dtoList = salesInvoiceService.getInvoicesDtoByBranch(branchId);
        return ApiResponse.success(dtoList);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STAFF')")
    public ApiResponse<com.storename.erp.order.api.dto.SalesInvoiceResponseDto> getInvoice(@PathVariable UUID id) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        com.storename.erp.order.api.dto.SalesInvoiceResponseDto invoiceDto = salesInvoiceService.getInvoiceDto(id, branchId);
        return ApiResponse.success(invoiceDto);
    }
}



