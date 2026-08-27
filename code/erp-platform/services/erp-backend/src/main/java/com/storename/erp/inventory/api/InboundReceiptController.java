package com.storename.erp.inventory.api;

import com.storename.erp.common.aop.BranchScoped;
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
public class InboundReceiptController {

    private final InboundReceiptService inboundService;

    private Long getBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails details) {
            return Long.parseLong(details.getBranchId());
        }
        throw new IllegalStateException("Branch ID not found in security context");
    }

    private UUID getUserId() {
        return UUID.randomUUID(); 
    }

    @PostMapping
    @BranchScoped
    @Operation(summary = "Tạo phiếu nhập kho (DRAFT)")
    public ResponseEntity<ApiResponse<UUID>> createDraft(@Valid @RequestBody InboundReceiptCreateDto dto) {
        Long branchId = getBranchId();
        UUID receiptId = inboundService.createDraft(branchId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, receiptId, "Inbound receipt draft created successfully", null));
    }

    @PostMapping("/{id}/confirm")
    @BranchScoped
    @com.storename.erp.common.aop.IdempotencyProtected
    @Operation(summary = "Xác nhận phiếu nhập kho (CONFIRM) và tăng tồn kho")
    public ResponseEntity<ApiResponse<Void>> confirmReceipt(@PathVariable("id") UUID receiptId) {
        Long branchId = getBranchId();
        UUID userId = getUserId();
        inboundService.confirmReceipt(receiptId, branchId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Inbound receipt confirmed successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<List<InboundReceipt>> getReceipts() {
        Long branchId = getBranchId();
        return ApiResponse.success(inboundService.getReceiptsByBranch(branchId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<InboundReceipt> getReceipt(@PathVariable UUID id) {
        Long branchId = getBranchId();
        return ApiResponse.success(inboundService.getReceipt(id, branchId));
    }
}
