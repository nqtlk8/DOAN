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
@Tag(name = "Inbound Receipt", description = "Quáº£n lÃ½ phiáº¿u nháº­p kho")
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'BRANCH' or '${instance.role:ALL}' == 'ALL'")
@Slf4j
public class InboundReceiptController {

    private final InboundReceiptService inboundService;



    private UUID getUserId() {
        return com.storename.erp.common.security.AuthUtils.getUserId();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    @com.storename.erp.common.aop.IdempotencyProtected
    @Operation(summary = "Táº¡o phiáº¿u nháº­p kho (DRAFT)")
    public ApiResponse<com.storename.erp.inventory.api.dto.InboundReceiptCreateResponseDto> createDraft(@Valid @RequestBody InboundReceiptCreateDto dto) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        UUID receiptId = inboundService.createDraft(branchId, dto,
                com.storename.erp.common.aop.IdempotencyContext.currentRequestKey());
        return new ApiResponse<>(true, new com.storename.erp.inventory.api.dto.InboundReceiptCreateResponseDto(receiptId), "Inbound receipt draft created successfully", null);
    }

    /** Sửa phiếu nhập khi còn nháp (DRAFT). Phiếu đã xác nhận -> 409 INVALID_STATUS. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STAFF')")
    @com.storename.erp.common.aop.IdempotencyProtected
    @Operation(summary = "Sửa phiếu nhập kho khi còn nháp (DRAFT)")
    public ApiResponse<com.storename.erp.inventory.api.dto.InboundReceiptCreateResponseDto> updateDraft(
            @PathVariable("id") UUID receiptId, @Valid @RequestBody InboundReceiptCreateDto dto) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        InboundReceipt receipt = inboundService.updateDraft(receiptId, branchId, dto);
        return new ApiResponse<>(true, new com.storename.erp.inventory.api.dto.InboundReceiptCreateResponseDto(receipt.getId()), "Inbound receipt draft updated successfully", null);
    }

    @PostMapping("/{id}/confirm")
    @com.storename.erp.common.aop.IdempotencyProtected
    @PreAuthorize("hasAuthority('STAFF')")
    @Operation(summary = "XÃ¡c nháº­n phiáº¿u nháº­p kho (CONFIRM) vÃ  tÄƒng tá»“n kho")
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



