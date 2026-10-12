package com.storename.erp.crm.api;

import com.storename.erp.common.aop.IdempotencyProtected;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.AuthUtils;
import com.storename.erp.crm.api.dto.DebtPaymentRequestDto;
import com.storename.erp.crm.application.ReceivableDebtService;
import com.storename.erp.crm.application.dto.ReceivableDebtResponseDto;
import com.storename.erp.crm.domain.ReceivableDebt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Thu nợ độc lập tại chi nhánh. Chỉ bật ở instance BRANCH/ALL vì công nợ do chi nhánh sở hữu
 * (receivable_debt, receivable_debt_movement đồng bộ Branch -> HQ); HQ không ghi công nợ.
 */
@RestController
@RequestMapping("/api/v1/receivable-debts")
@RequiredArgsConstructor
@Tag(name = "Receivable Debt", description = "Quản lý công nợ khách hàng")
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'BRANCH' or '${instance.role:ALL}' == 'ALL'")
public class DebtPaymentController {

    private final ReceivableDebtService debtService;

    @PostMapping("/{customerId}/payments")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @IdempotencyProtected
    @Operation(summary = "Thu nợ: ghi biến động PAYMENT, giảm công nợ của khách tại chi nhánh hiện tại")
    public ApiResponse<ReceivableDebtResponseDto> recordPayment(
            @PathVariable UUID customerId,
            @Valid @RequestBody DebtPaymentRequestDto request) {
        // Bắt buộc token có chi nhánh: công nợ được ghi theo (khách, chi nhánh).
        Long branchId = AuthUtils.getBranchId();
        UUID userId = AuthUtils.getUserId();
        ReceivableDebt debt = debtService.recordPayment(customerId, branchId, request.getAmount(), userId, request.getNote());
        return ApiResponse.success(ReceivableDebtResponseDto.fromEntity(debt), "Debt payment recorded");
    }
}
