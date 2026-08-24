package com.storename.erp.order.api;

import com.storename.erp.common.aop.BranchScoped;
import com.storename.erp.common.aop.IdempotencyProtected;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.order.application.CustomerOrderService;
import com.storename.erp.order.application.dto.CustomerOrderCreateDto;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Customer Order", description = "Quản lý đơn đặt hàng của khách")
public class CustomerOrderController {

    private final CustomerOrderService orderService;

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
    @Operation(summary = "Tạo đơn hàng nháp (DRAFT)")
    public ResponseEntity<ApiResponse<UUID>> createDraft(@Valid @RequestBody CustomerOrderCreateDto dto) {
        UUID branchId = getBranchId();
        UUID orderId = orderService.createDraft(branchId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, orderId, "Customer order draft created successfully", null));
    }

    @PostMapping("/{id}/confirm")
    @BranchScoped
    @IdempotencyProtected
    @Operation(summary = "Xác nhận đơn hàng (CONFIRM) và tạo Hoá đơn")
    public ResponseEntity<ApiResponse<Void>> confirmOrder(@PathVariable("id") UUID orderId) {
        UUID branchId = getBranchId();
        UUID userId = getUserId();
        orderService.confirmOrder(orderId, branchId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Customer order confirmed successfully", null));
    }
}
