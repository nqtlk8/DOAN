package com.storename.erp.inventory.api;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.inventory.application.InboundReceiptService;
import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import com.storename.erp.inventory.domain.InboundReceipt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/inbound")
@RequiredArgsConstructor
@Tag(name = "Inbound Receipt", description = "Quản lý phiếu nhập kho")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "instance.role", havingValue = "BRANCH")
@Slf4j
public class InboundReceiptController {

    private final InboundReceiptService inboundService;



    private UUID getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo phiếu nhập kho (DRAFT)")
    public ApiResponse<UUID> createDraft(@Valid @RequestBody InboundReceiptCreateDto dto) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        UUID receiptId = inboundService.createDraft(branchId, dto);
        return new ApiResponse<>(true, receiptId, "Inbound receipt draft created successfully", null);
    }

    @PostMapping("/{id}/confirm")
    @com.storename.erp.common.aop.IdempotencyProtected
    @PreAuthorize("hasAuthority('STAFF')")
    @Operation(summary = "Xác nhận phiếu nhập kho (CONFIRM) và tăng tồn kho")
    public ApiResponse<Void> confirmReceipt(@PathVariable("id") UUID receiptId) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        UUID userId = getUserId();
        inboundService.confirmReceipt(receiptId, branchId, userId);
        return new ApiResponse<>(true, null, "Inbound receipt confirmed successfully", null);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<com.storename.erp.inventory.api.dto.InboundReceiptResponseDto>> getReceipts() {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        List<com.storename.erp.inventory.api.dto.InboundReceiptResponseDto> dtoList = inboundService.getReceiptsByBranch(branchId)
                .stream().map(com.storename.erp.inventory.api.dto.InboundReceiptResponseDto::fromEntity)
                .toList();
        return ApiResponse.success(dtoList);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<com.storename.erp.inventory.api.dto.InboundReceiptResponseDto> getReceipt(@PathVariable UUID id) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        InboundReceipt receipt = inboundService.getReceipt(id, branchId);
        return ApiResponse.success(com.storename.erp.inventory.api.dto.InboundReceiptResponseDto.fromEntity(receipt));
    }
}



