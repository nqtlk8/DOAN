package com.storename.erp.inventory.api;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class StockController {
    
    private final StockOnHandRepository stockRepo;



    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<StockOnHand>> getStock() {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        if (branchId == null) {
            return ApiResponse.success(stockRepo.findAll());
        }
        return ApiResponse.success(stockRepo.findByBranchId(branchId));
    }
}



