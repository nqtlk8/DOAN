package com.storename.erp.crm.api;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.crm.domain.ReceivableDebt;
import com.storename.erp.crm.domain.ReceivableDebtMovement;
import com.storename.erp.crm.infrastructure.ReceivableDebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/receivable-debts")
@RequiredArgsConstructor
@Slf4j
public class ReceivableDebtController {

    private final ReceivableDebtRepository debtRepository;
    private final com.storename.erp.crm.application.ReceivableDebtService debtService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<com.storename.erp.crm.application.dto.ReceivableDebtResponseDto>> getDebts() {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        List<ReceivableDebt> debts;
        if (branchId == null) {
            debts = debtRepository.findAll();
        } else {
            debts = debtRepository.findByBranchId(branchId);
        }
        
        List<com.storename.erp.crm.application.dto.ReceivableDebtResponseDto> response = debts.stream()
                .map(com.storename.erp.crm.application.dto.ReceivableDebtResponseDto::fromEntity)
                .toList();
        
        return ApiResponse.success(response);
    }

    @PostMapping("/opening-balance")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<com.storename.erp.crm.application.dto.ReceivableDebtResponseDto> setOpeningBalance(
            @RequestBody com.storename.erp.crm.api.dto.OpeningBalanceRequestDto request) {
        
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        if (branchId == null) {
            throw new SecurityException("Branch ID is required to set opening balance");
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        java.util.UUID userId = null;
        if (authentication != null && authentication.getName() != null) {
            try {
                userId = java.util.UUID.fromString(authentication.getName());
            } catch (Exception e) {
                log.warn("Could not parse userId from auth name", e);
            }
        }
        
        ReceivableDebt debt = debtService.setOpeningBalance(
                request.getCustomerId(), branchId, request.getAmount(), userId, request.getNote());
                
        return ApiResponse.success(com.storename.erp.crm.application.dto.ReceivableDebtResponseDto.fromEntity(debt));
    }

    @GetMapping("/{customerId}/movements")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<com.storename.erp.crm.application.dto.ReceivableDebtMovementResponseDto>> getMovements(
            @PathVariable java.util.UUID customerId) {
        
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        if (branchId == null) {
            throw new SecurityException("Branch ID is required to fetch movements");
        }

        List<ReceivableDebtMovement> movements = debtService.getMovements(customerId, branchId);
        List<com.storename.erp.crm.application.dto.ReceivableDebtMovementResponseDto> response = movements.stream()
                .map(com.storename.erp.crm.application.dto.ReceivableDebtMovementResponseDto::fromEntity)
                .toList();

        return ApiResponse.success(response);
    }

    @GetMapping("/{customerId}/balance")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<java.math.BigDecimal> getBalance(@PathVariable java.util.UUID customerId) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        if (branchId == null) {
            throw new SecurityException("Branch ID is required to fetch balance");
        }
        java.math.BigDecimal balance = debtService.getCurrentDebt(customerId, branchId);
        return ApiResponse.success(balance);
    }
}
