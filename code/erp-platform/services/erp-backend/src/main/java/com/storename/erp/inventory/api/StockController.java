package com.storename.erp.inventory.api;

import com.storename.erp.common.aop.BranchScoped;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/stock")
@RequiredArgsConstructor
@BranchScoped
public class StockController {
    
    private final StockOnHandRepository stockRepo;

    private Long getBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails details) {
            return Long.parseLong(details.getBranchId());
        }
        throw new IllegalStateException("Branch ID not found in security context");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ApiResponse<List<StockOnHand>> getStock() {
        return ApiResponse.success(stockRepo.findByBranchId(getBranchId()));
    }
}
