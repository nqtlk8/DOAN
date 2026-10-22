package com.storename.erp.order.api;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.security.AuthUtils;
import com.storename.erp.order.application.CustomerProductPriceService;
import com.storename.erp.order.application.dto.ResolvedPriceDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer-prices")
@Slf4j
public class CustomerProductPriceController {

    /** Giới hạn số sản phẩm mỗi lần tra giá để tránh URL/truy vấn quá lớn. */
    private static final int MAX_PRODUCTS_PER_REQUEST = 500;

    private final CustomerProductPriceService priceService;

    public CustomerProductPriceController(CustomerProductPriceService priceService) {
        this.priceService = priceService;
    }

    /**
     * Giá áp dụng cho nhiều sản phẩm khi bán cho khách tại chi nhánh hiện tại.
     * Ưu tiên giá riêng của khách, sau đó giá niêm yết chi nhánh.
     * Ví dụ: GET /api/v1/customer-prices/{customerId}/resolve?productIds=1,2,3
     */
    @GetMapping("/{customerId}/resolve")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<ResolvedPriceDto>> resolvePrices(
            @PathVariable UUID customerId,
            @RequestParam List<Long> productIds) {

        if (productIds.size() > MAX_PRODUCTS_PER_REQUEST) {
            throw new IllegalArgumentException("Tối đa " + MAX_PRODUCTS_PER_REQUEST + " sản phẩm mỗi lần tra giá");
        }
        // branchId nằm trong Authentication.details (JwtAuthDetails), principal là username —
        // không dùng @AuthenticationPrincipal JwtAuthDetails (luôn null => NPE/500).
        Long branchId = AuthUtils.getBranchId();
        return ApiResponse.success(priceService.resolvePrices(customerId, branchId, productIds));
    }
}
