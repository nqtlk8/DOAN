package com.storename.erp.procurement.api;

import com.storename.erp.common.aop.BranchScoped;
import com.storename.erp.common.aop.IdempotencyProtected;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.procurement.application.StockTransferService;
import com.storename.erp.procurement.application.dto.StockTransferCreateDto;
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
@RequestMapping("/api/v1/procurement/stock-transfers")
@RequiredArgsConstructor
@Tag(name = "Stock Transfer", description = "Quản lý phiếu chuyển kho")
public class StockTransferController {

    private final StockTransferService transferService;

    private UUID getBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails details) {
            return UUID.fromString(details.getBranchId());
        }
        throw new IllegalStateException("Branch ID not found in security context");
    }

    private UUID getUserId() {
        return UUID.randomUUID();
    }

    @PostMapping
    @BranchScoped
    @Operation(summary = "Tạo phiếu chuyển kho nháp (REQUESTED)")
    public ResponseEntity<ApiResponse<UUID>> requestTransfer(@Valid @RequestBody StockTransferCreateDto dto) {
        UUID fromBranchId = getBranchId();
        UUID transferId = transferService.requestTransfer(fromBranchId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, transferId, "Stock transfer requested successfully", null));
    }

    @PostMapping("/{id}/ship")
    @BranchScoped
    @IdempotencyProtected
    @Operation(summary = "Xuất kho chuyển hàng (SHIP) - Tại chi nhánh xuất")
    public ResponseEntity<ApiResponse<Void>> shipTransfer(@PathVariable("id") UUID transferId) {
        UUID branchId = getBranchId();
        UUID userId = getUserId();
        transferService.shipTransfer(transferId, branchId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Stock transfer shipped successfully", null));
    }

    @PostMapping("/{id}/receive")
    @BranchScoped
    @IdempotencyProtected
    @Operation(summary = "Nhập kho nhận hàng (RECEIVE) - Tại chi nhánh nhận")
    public ResponseEntity<ApiResponse<Void>> receiveTransfer(@PathVariable("id") UUID transferId) {
        UUID branchId = getBranchId();
        UUID userId = getUserId();
        transferService.receiveTransfer(transferId, branchId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Stock transfer received successfully", null));
    }
}
