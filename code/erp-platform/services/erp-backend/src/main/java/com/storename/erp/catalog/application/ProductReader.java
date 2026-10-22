package com.storename.erp.catalog.application;

import com.storename.erp.catalog.application.dto.ProductResponseDto;
import com.storename.erp.catalog.domain.PriceList;
import com.storename.erp.catalog.domain.Product;
import com.storename.erp.catalog.infrastructure.PriceListRepository;
import com.storename.erp.catalog.infrastructure.ProductRepository;
import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.common.security.JwtAuthDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReader {

    private final ProductRepository productRepository;
    private final PriceListRepository priceListRepository;

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProductsWithBranchPrice() {
        return getAllProductsWithBranchPrice(null);
    }

    /**
     * Danh sách sản phẩm kèm giá niêm yết của một chi nhánh.
     * Người dùng gắn chi nhánh (nhân viên) luôn nhận giá chi nhánh trong token; {@code requestedBranchId}
     * bị bỏ qua để không xem được bảng giá chi nhánh khác. Người dùng không gắn chi nhánh (quản trị viên HQ)
     * chọn chi nhánh qua {@code requestedBranchId}; không truyền thì giá để trống.
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProductsWithBranchPrice(Long requestedBranchId) {
        log.info("Fetching all products with branch price");
        Long tokenBranchId = getCurrentBranchId();
        Long branchId = tokenBranchId != null ? tokenBranchId : requestedBranchId;

        List<Product> products = productRepository.findAll();
        List<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        Map<Long, BigDecimal> priceMap = loadBranchPrices(branchId, productIds);

        return products.stream()
                .map(product -> {
                    ProductResponseDto dto = mapToResponse(product);
                    dto.setPrice(priceMap.get(product.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Giá niêm yết của chi nhánh: productId -> price. Mỗi cặp (sản phẩm, chi nhánh) có đúng một giá
     * (ràng buộc uk_price_list_product_branch, migration V12). Nếu dữ liệu cũ còn dòng trùng thì lấy
     * dòng có id lớn nhất thay vì ném lỗi, để một dòng trùng không làm hỏng cả danh sách sản phẩm.
     */
    private Map<Long, BigDecimal> loadBranchPrices(Long branchId, Collection<Long> productIds) {
        if (branchId == null || productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, PriceList> latest = new HashMap<>();
        for (PriceList p : priceListRepository.findByBranchIdAndProductIdIn(branchId, productIds)) {
            latest.merge(p.getProduct().getId(), p, (a, b) -> a.getId() >= b.getId() ? a : b);
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        latest.forEach((productId, p) -> result.put(productId, p.getPrice()));
        return result;
    }

    private ProductResponseDto mapToResponse(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .baseUnit(product.getBaseUnit())
                .isActive(product.isActive())
                .attributes(product.getAttributesCache())
                .build();
    }

    private Long getCurrentBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails) {
            String branchIdStr = ((JwtAuthDetails) auth.getDetails()).getBranchId();
            if (branchIdStr != null && !branchIdStr.trim().isEmpty()) {
                try {
                    return Long.parseLong(branchIdStr);
                } catch (NumberFormatException e) {
                    log.error("Invalid branch ID format: {}", branchIdStr);
                }
            }
        }
        return null;
    }
}
