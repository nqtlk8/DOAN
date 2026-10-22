package com.storename.erp.order.api;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.common.aop.IdempotencyContext;
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

    // Trả về ApiResponse (không bọc ResponseEntity): IdempotencyAspect lưu phản hồi dạng JSON và đọc lại theo
    // kiểu trả về của phương thức khi client gửi lại cùng Idempotency-Key; ResponseEntity không đọc lại được.
    @PostMapping
    @PreAuthorize("hasAuthority('STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    @IdempotencyProtected
    @Operation(summary = "Tạo phiếu trả hàng (DRAFT)")
    public ApiResponse<UUID> createDraft(@Valid @RequestBody GoodsReturnCreateDto dto) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        UUID returnId = returnService.createDraft(branchId, dto, IdempotencyContext.currentRequestKey());
        return new ApiResponse<>(true, returnId, "Goods return draft created successfully", null);
    }

    /** Sửa phiếu trả khi còn nháp (DRAFT). Phiếu đã xác nhận -> 409 INVALID_STATUS. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STAFF')")
    @IdempotencyProtected
    @Operation(summary = "Sửa phiếu trả hàng khi còn nháp (DRAFT)")
    public ApiResponse<UUID> updateDraft(@PathVariable("id") UUID returnId, @Valid @RequestBody GoodsReturnCreateDto dto) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        UUID id = returnService.updateDraft(returnId, branchId, dto).getId();
        return new ApiResponse<>(true, id, "Goods return draft updated successfully", null);
    }

    @PostMapping("/{id}/confirm")
    @IdempotencyProtected
    @PreAuthorize("hasAuthority('STAFF')")
    @Operation(summary = "Xác nhận trả hàng (CONFIRM) và hoàn kho")
    public ApiResponse<Void> confirmReturn(@PathVariable("id") UUID returnId) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        UUID userId = getUserId();
        returnService.confirmReturn(returnId, branchId, userId);
        return new ApiResponse<>(true, null, "Goods return confirmed successfully", null);
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



