package com.storename.erp.order.api;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.common.api.ApiResponse;
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

    @PostMapping
    @PreAuthorize("hasAuthority('STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    @IdempotencyProtected
    public ApiResponse<com.storename.erp.order.application.dto.SalesInvoiceCreateResponseDto> createInvoice(
            @Valid @RequestBody SalesInvoiceCreateDto dto) {

        // Bấm "Lưu" tạo đơn và xác nhận luôn trong cùng 1 giao dịch: trừ tồn kho + cộng
        // công nợ ngay lập tức, không để lại đơn ở trạng thái DRAFT chưa xác nhận.
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        UUID userId = getUserId();
        SalesInvoice invoice = salesInvoiceService.createAndConfirm(dto, branchId, userId);
        return ApiResponse.success(new com.storename.erp.order.application.dto.SalesInvoiceCreateResponseDto(invoice.getId(), invoice.getInvoiceCode()), "Sales invoice created and confirmed successfully");
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('STAFF')")
    @IdempotencyProtected
    public ApiResponse<com.storename.erp.order.application.dto.SalesInvoiceCreateResponseDto> confirmInvoice(
            @PathVariable UUID id) {
        
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        UUID userId = getUserId();
        
        SalesInvoice invoice = salesInvoiceService.confirmInvoice(id, userId, branchId);
        return ApiResponse.success(new com.storename.erp.order.application.dto.SalesInvoiceCreateResponseDto(invoice.getId(), invoice.getInvoiceCode()), "Sales invoice confirmed successfully");
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



