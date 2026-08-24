package com.storename.erp.procurement.api;

import com.storename.erp.common.aop.BranchScoped;
import com.storename.erp.common.aop.IdempotencyProtected;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.procurement.application.SupplierPurchaseOrderService;
import com.storename.erp.procurement.application.dto.SupplierPurchaseOrderCreateDto;
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
@RequestMapping("/api/v1/procurement/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Order", description = "Quản lý đơn đặt hàng nhà cung cấp")
public class SupplierPurchaseOrderController {

    private final SupplierPurchaseOrderService poService;

    private UUID getBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails details) {
            return UUID.fromString(details.getBranchId());
        }
        throw new IllegalStateException("Branch ID not found in security context");
    }

    @PostMapping
    @BranchScoped
    @Operation(summary = "Tạo đơn đặt hàng nháp (DRAFT)")
    public ResponseEntity<ApiResponse<UUID>> createDraft(@Valid @RequestBody SupplierPurchaseOrderCreateDto dto) {
        UUID branchId = getBranchId();
        UUID poId = poService.createDraft(branchId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, poId, "Purchase order draft created successfully", null));
    }

    @PostMapping("/{id}/confirm")
    @BranchScoped
    @IdempotencyProtected
    @Operation(summary = "Xác nhận đơn đặt hàng (CONFIRM)")
    public ResponseEntity<ApiResponse<Void>> confirmOrder(@PathVariable("id") UUID poId) {
        UUID branchId = getBranchId();
        poService.confirmOrder(poId, branchId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Purchase order confirmed successfully", null));
    }
}
