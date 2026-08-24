package com.storename.erp.catalog.application;

import com.storename.erp.catalog.domain.Category;
import com.storename.erp.catalog.infrastructure.CategoryRepository;
import com.storename.erp.catalog.infrastructure.ProductRepository;
import com.storename.erp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")
@RequiredArgsConstructor
public class CategoryWriter {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    /**
     * Deletes a category.
     * Prevents deletion if the category has child categories or products associated with it.
     */
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Request to delete category: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (!category.getChildren().isEmpty()) {
            throw new IllegalStateException("Cannot delete category because it has child categories.");
        }

        long productCount = productRepository.countByCategory(category);
        if (productCount > 0) {
            throw new IllegalStateException("Cannot delete category because it has " + productCount + " products associated.");
        }

        categoryRepository.delete(category);
        log.info("Successfully deleted category: {}", id);
    }
}
