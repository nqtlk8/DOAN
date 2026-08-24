package com.storename.erp.order.api;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.aop.BranchScoped;
import com.storename.erp.common.security.JwtAuthDetails;
import com.storename.erp.order.application.CustomerProductPriceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer-prices")
@BranchScoped
public class CustomerProductPriceController {

    private final CustomerProductPriceService priceService;

    public CustomerProductPriceController(CustomerProductPriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping("/{customerId}/product/{productId}")
    @PreAuthorize("hasAnyAuthority('SALE_READ', 'ADMIN')")
    public ApiResponse<BigDecimal> getPrice(
            @PathVariable UUID customerId,
            @PathVariable UUID productId,
            @AuthenticationPrincipal JwtAuthDetails authDetails) {
        
        UUID branchId = UUID.fromString(authDetails.getBranchId());
        
        return priceService.getPrice(customerId, productId, branchId)
                .map(price -> ApiResponse.success(price, "Price found"))
                .orElseGet(() -> ApiResponse.success(null, "No special price found"));
    }
}
