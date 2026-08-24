package com.storename.erp.inventory.api;

import com.storename.erp.common.aop.BranchScoped;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.inventory.application.OutboundReceiptService;
import com.storename.erp.inventory.application.dto.OutboundReceiptCreateDto;
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
@RequestMapping("/api/v1/inventory/outbound")
@RequiredArgsConstructor
@Tag(name = "Outbound Receipt", description = "Quản lý phiếu xuất kho")
public class OutboundReceiptController {

    private final OutboundReceiptService outboundService;

    private UUID getBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails details) {
            return UUID.fromString(details.getBranchId());
        }
        throw new IllegalStateException("Branch ID not found in security context");
    }

    private UUID getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return UUID.nameUUIDFromBytes(auth.getName().getBytes());
        }
        return UUID.randomUUID(); 
    }

    @PostMapping
    @BranchScoped
    @Operation(summary = "Tạo phiếu xuất kho nháp (DRAFT)")
    public ResponseEntity<ApiResponse<UUID>> createDraft(@Valid @RequestBody OutboundReceiptCreateDto dto) {
        UUID branchId = getBranchId();
        UUID receiptId = outboundService.createDraft(branchId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, receiptId, "Outbound receipt draft created successfully", null));
    }

    @PostMapping("/{id}/confirm")
    @BranchScoped
    @Operation(summary = "Xác nhận phiếu xuất kho (CONFIRM) và giảm tồn kho")
    @org.springframework.retry.annotation.Retryable(
        retryFor = org.springframework.orm.ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 100)
    )
    public ResponseEntity<ApiResponse<Void>> confirmReceipt(@PathVariable("id") UUID receiptId) {
        UUID branchId = getBranchId();
        UUID userId = getUserId();
        outboundService.confirmReceipt(receiptId, branchId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Outbound receipt confirmed successfully", null));
    }
}
