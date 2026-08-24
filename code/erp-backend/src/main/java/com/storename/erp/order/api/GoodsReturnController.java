package com.storename.erp.order.api;

import com.storename.erp.common.aop.BranchScoped;
import com.storename.erp.common.aop.IdempotencyProtected;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.order.application.GoodsReturnService;
import com.storename.erp.order.application.dto.GoodsReturnCreateDto;
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
@RequestMapping("/api/v1/goods-returns")
@RequiredArgsConstructor
@Tag(name = "Goods Return", description = "Quản lý khách trả hàng")
public class GoodsReturnController {

    private final GoodsReturnService returnService;

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
    @Operation(summary = "Tạo phiếu trả hàng nháp (DRAFT)")
    public ResponseEntity<ApiResponse<UUID>> createDraft(@Valid @RequestBody GoodsReturnCreateDto dto) {
        UUID branchId = getBranchId();
        UUID returnId = returnService.createDraft(branchId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, returnId, "Goods return draft created successfully", null));
    }

    @PostMapping("/{id}/confirm")
    @BranchScoped
    @IdempotencyProtected
    @Operation(summary = "Xác nhận trả hàng (CONFIRM) và hoàn kho")
    public ResponseEntity<ApiResponse<Void>> confirmReturn(@PathVariable("id") UUID returnId) {
        UUID branchId = getBranchId();
        UUID userId = getUserId();
        returnService.confirmReturn(returnId, branchId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Goods return confirmed successfully", null));
    }
}
