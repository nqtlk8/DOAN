package com.storename.erp.order.api;

import lombok.extern.slf4j.Slf4j;
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
@Tag(name = "Goods Return", description = "Quáº£n lÃ½ khÃ¡ch tráº£ hÃ ng")
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'BRANCH' or '${instance.role:ALL}' == 'ALL'")
@Slf4j
public class GoodsReturnController {

    private final GoodsReturnService returnService;



    private UUID getUserId() {
        return com.storename.erp.common.security.AuthUtils.getUserId();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STAFF')")
    @IdempotencyProtected
    @Operation(summary = "Táº¡o phiáº¿u tráº£ hÃ ng (DRAFT)")
    public ResponseEntity<ApiResponse<UUID>> createDraft(@Valid @RequestBody GoodsReturnCreateDto dto) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        UUID returnId = returnService.createDraft(branchId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, returnId, "Goods return draft created successfully", null));
    }

    @PostMapping("/{id}/confirm")
    @IdempotencyProtected
    @PreAuthorize("hasAuthority('STAFF')")
    @Operation(summary = "XÃ¡c nháº­n tráº£ hÃ ng (CONFIRM) vÃ  hoÃ n kho")
    public ResponseEntity<ApiResponse<Void>> confirmReturn(@PathVariable("id") UUID returnId) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        UUID userId = getUserId();
        returnService.confirmReturn(returnId, branchId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Goods return confirmed successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<java.util.List<com.storename.erp.order.api.dto.GoodsReturnResponseDto>> getReturns() {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        return ApiResponse.success(returnService.getReturnsByBranch(branchId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<com.storename.erp.order.api.dto.GoodsReturnResponseDto> getReturn(@PathVariable UUID id) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        return ApiResponse.success(returnService.getReturnDto(id, branchId));
    }
}



