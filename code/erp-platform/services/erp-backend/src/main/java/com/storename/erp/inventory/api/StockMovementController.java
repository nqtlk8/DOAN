package com.storename.erp.inventory.api;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.inventory.application.StockQueryService;
import com.storename.erp.inventory.api.dto.StockMovementResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for querying stock movements (Inventory Ledger).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockQueryService queryService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<StockMovementResponseDto>> getMovements(@RequestParam Long productId) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        log.info("REST request to get stock movements for product {} in branch {}", productId, branchId);
        List<StockMovementResponseDto> movements = queryService.getStockMovements(productId, branchId);
        return ApiResponse.success(movements);
    }

    
}


