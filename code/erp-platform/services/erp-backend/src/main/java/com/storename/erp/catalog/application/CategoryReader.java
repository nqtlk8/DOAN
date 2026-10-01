package com.storename.erp.catalog.application;

import com.storename.erp.catalog.application.dto.CategoryResponseDto;
import com.storename.erp.catalog.domain.Category;
import com.storename.erp.catalog.infrastructure.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Đọc danh mục sản phẩm. Chạy ở cả HQ và Branch (Branch đọc bản replicate từ HQ).
 * Danh mục là master data do HQ quản lý; dữ liệu ban đầu nằm trong
 * {@code db/migration-hq/R__reference_data.sql}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryReader {

    private final CategoryRepository categoryRepository;

    /** Mọi danh mục đang hoạt động, danh mục gốc trước, sau đó theo id (thứ tự nhập). */
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getActiveCategories() {
        List<CategoryResponseDto> result = categoryRepository.findAllActiveWithParent().stream()
                .map(CategoryReader::toDto)
                .toList();
        log.debug("Returning {} categories", result.size());
        return result;
    }

    static CategoryResponseDto toDto(Category category) {
        Category parent = category.getParent();
        return CategoryResponseDto.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .parentId(parent != null ? parent.getId() : null)
                .parentName(parent != null ? parent.getName() : null)
                .isActive(category.isActive())
                .build();
    }
}
