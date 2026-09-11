package com.storename.erp.order.api;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goods-returns")
@RequiredArgsConstructor
@Tag(name = "Goods Return", description = "Quản lý khách trả hàng")
@BranchScoped
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "instance.role", havingValue = "BRANCH")
@Slf4j
public class GoodsReturnController {

    private final GoodsReturnService returnService;



    private UUID getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }

    @PostMapping
    @BranchScoped
    @PreAuthorize("hasAuthority('STAFF')")
    @Operation(summary = "Tạo phiếu trả hàng (DRAFT)")
    public ResponseEntity<ApiResponse<UUID>> createDraft(@Valid @RequestBody GoodsReturnCreateDto dto) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchId();
        UUID returnId = returnService.createDraft(branchId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, returnId, "Goods return draft created successfully", null));
    }

    @PostMapping("/{id}/confirm")
    @BranchScoped
    @IdempotencyProtected
    @PreAuthorize("hasAuthority('STAFF')")
    @Operation(summary = "Xác nhận trả hàng (CONFIRM) và hoàn kho")
    public ResponseEntity<ApiResponse<Void>> confirmReturn(@PathVariable("id") UUID returnId) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchId();
        UUID userId = getUserId();
        returnService.confirmReturn(returnId, branchId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Goods return confirmed successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<java.util.List<com.storename.erp.order.domain.GoodsReturn>> getReturns() {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchId();
        return ApiResponse.success(returnService.getReturnsByBranch(branchId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<com.storename.erp.order.domain.GoodsReturn> getReturn(@PathVariable UUID id) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchId();
        return ApiResponse.success(returnService.getReturn(id, branchId));
    }
}


