package com.storename.erp.crm.api;

import com.storename.erp.common.aop.BranchScoped;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.crm.domain.ReceivableDebt;
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
@BranchScoped
public class ReceivableDebtController {

    private final ReceivableDebtRepository debtRepository;

    private Long getBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails details) {
            return Long.parseLong(details.getBranchId());
        }
        throw new IllegalStateException("Branch ID not found in security context");
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<ReceivableDebt>> getDebts() {
        return ApiResponse.success(debtRepository.findByBranchId(getBranchId()));
    }
}
