package com.storename.erp.catalog.api;

import com.storename.erp.catalog.application.CategoryReader;
import com.storename.erp.catalog.application.dto.CategoryResponseDto;
import com.storename.erp.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API đọc danh mục sản phẩm (dropdown khi tạo/sửa sản phẩm). Chạy ở cả HQ và Branch.
 * Đường dẫn nằm dưới {@code /api/v1/catalog} nên gateway chi nhánh chuyển GET tới app chi nhánh.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/catalog/categories")
@RequiredArgsConstructor
public class CategoryReadController {

    private final CategoryReader categoryReader;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<CategoryResponseDto>> getCategories() {
        log.info("REST request to get categories");
        return ApiResponse.success(categoryReader.getActiveCategories());
    }
}
