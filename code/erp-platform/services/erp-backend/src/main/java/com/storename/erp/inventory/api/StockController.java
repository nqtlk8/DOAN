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
    private final com.storename.erp.inventory.application.StockService stockService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<com.storename.erp.inventory.application.dto.StockOnHandResponseDto>> getStock() {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        return ApiResponse.success(stockService.getStockByBranch(branchId));
    }
}



