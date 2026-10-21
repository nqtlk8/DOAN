package com.storename.erp.catalog.api;

import com.storename.erp.catalog.application.ProductReader;
import com.storename.erp.catalog.application.dto.ProductResponseDto;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

/**
 * Danh mục sản phẩm công khai (không cần đăng nhập, xem SecurityConfig: /api/v1/public/**).
 * Chỉ trả sản phẩm đang bán (isActive = true). Hỗ trợ lọc theo từ khóa (tên hoặc mã),
 * theo danh mục và phân trang (page bắt đầu từ 0, size từ 1 đến {@value #MAX_PAGE_SIZE}).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/public/catalog")
@RequiredArgsConstructor
public class PublicCatalogController {

    static final int MAX_PAGE_SIZE = 100;

    private final ProductReader productReader;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponseDto>>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Public REST request to get products: search={}, categoryId={}, page={}, size={}",
                search, categoryId, page, size);
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        List<ProductResponseDto> products = productReader.getAllProducts().stream()
                .filter(ProductResponseDto::isActive)
                .filter(p -> categoryId == null || categoryId.equals(p.getCategoryId()))
                .filter(p -> term.isEmpty() || contains(p.getName(), term) || contains(p.getCode(), term))
                .skip((long) page * size)
                .limit(size)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    private static boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }
}
