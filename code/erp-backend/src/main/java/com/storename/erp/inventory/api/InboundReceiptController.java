package com.storename.erp.inventory.api;

import com.storename.erp.common.aop.BranchScoped;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.inventory.application.InboundReceiptService;
import com.storename.erp.inventory.application.dto.InboundReceiptCreateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/inbound")
@RequiredArgsConstructor
@Tag(name = "Inbound Receipt", description = "Quản lý phiếu nhập kho")
public class InboundReceiptController {

    private final InboundReceiptService inboundService;

    private UUID getBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails details) {
            return UUID.fromString(details.getBranchId());
        }
        throw new IllegalStateException("Branch ID not found in security context");
    }

    private UUID getUserId() {
        // Tạm thời mock userId, thực tế lấy từ authentication principal
        return UUID.randomUUID(); 
    }

    @PostMapping
    @BranchScoped
    @Operation(summary = "Tạo phiếu nhập kho nháp (DRAFT)")
    public ResponseEntity<ApiResponse<UUID>> createDraft(@Valid @RequestBody InboundReceiptCreateDto dto) {
        UUID branchId = getBranchId();
        UUID receiptId = inboundService.createDraft(branchId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, receiptId, "Inbound receipt draft created successfully", null));
    }

    @PostMapping("/{id}/confirm")
    @BranchScoped
    @com.storename.erp.common.aop.IdempotencyProtected
    @Operation(summary = "Xác nhận phiếu nhập kho (CONFIRM) và tăng tồn kho")
    public ResponseEntity<ApiResponse<Void>> confirmReceipt(@PathVariable("id") UUID receiptId) {
        UUID branchId = getBranchId();
        UUID userId = getUserId();
        inboundService.confirmReceipt(receiptId, branchId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Inbound receipt confirmed successfully", null));
    }
}
