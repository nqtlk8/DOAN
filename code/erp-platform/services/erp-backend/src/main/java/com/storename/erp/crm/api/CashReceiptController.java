package com.storename.erp.crm.api;

import com.storename.erp.common.aop.IdempotencyContext;
import com.storename.erp.common.aop.IdempotencyProtected;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.AuthUtils;
import com.storename.erp.crm.application.CashReceiptService;
import com.storename.erp.crm.application.dto.CashReceiptCancelDto;
import com.storename.erp.crm.application.dto.CashReceiptCreateDto;
import com.storename.erp.crm.application.dto.CashReceiptCreateResponseDto;
import com.storename.erp.crm.application.dto.CashReceiptResponseDto;
import com.storename.erp.crm.domain.CashReceipt;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Phiếu thu tiền khách hàng. Chỉ bật ở chi nhánh (chi nhánh là bên ghi duy nhất bảng giao dịch);
 * ADMIN ở HQ xem các lần thu qua sao kê công nợ.
 */
@RestController
@RequestMapping("/api/v1/cash-receipts")
@ConditionalOnExpression("'${instance.role:ALL}' == 'BRANCH' or '${instance.role:ALL}' == 'ALL'")
@RequiredArgsConstructor
@Tag(name = "Cash Receipt", description = "Phiếu thu tiền khách hàng")
public class CashReceiptController {

    private final CashReceiptService cashReceiptService;

    @PostMapping
    @PreAuthorize("hasAuthority('STAFF')")
    @ResponseStatus(HttpStatus.CREATED)
    @IdempotencyProtected
    public ApiResponse<CashReceiptCreateResponseDto> create(@Valid @RequestBody CashReceiptCreateDto dto) {
        CashReceipt receipt = cashReceiptService.create(dto, AuthUtils.getBranchIdOrNull(), AuthUtils.getUserId(),
                IdempotencyContext.currentRequestKey());
        return ApiResponse.success(new CashReceiptCreateResponseDto(receipt.getId(), receipt.getReceiptCode()),
                "Đã lập phiếu thu");
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('STAFF')")
    @IdempotencyProtected
    public ApiResponse<CashReceiptResponseDto> cancel(@PathVariable UUID id,
                                                      @Valid @RequestBody(required = false) CashReceiptCancelDto dto) {
        Long branchId = AuthUtils.getBranchIdOrNull();
        cashReceiptService.cancel(id, dto != null ? dto.getReason() : null, branchId, AuthUtils.getUserId());
        return ApiResponse.success(cashReceiptService.get(id, branchId), "Đã hủy phiếu thu");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STAFF')")
    public ApiResponse<List<CashReceiptResponseDto>> list() {
        return ApiResponse.success(cashReceiptService.list(AuthUtils.getBranchIdOrNull()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STAFF')")
    public ApiResponse<CashReceiptResponseDto> get(@PathVariable UUID id) {
        return ApiResponse.success(cashReceiptService.get(id, AuthUtils.getBranchIdOrNull()));
    }
}
