package com.storename.erp.catalog.application;

import com.storename.erp.catalog.application.dto.ProductCreateDto;
import com.storename.erp.catalog.application.dto.ProductUpdateDto;
import com.storename.erp.catalog.domain.Category;
import com.storename.erp.catalog.domain.Product;
import com.storename.erp.catalog.infrastructure.CategoryRepository;
import com.storename.erp.catalog.infrastructure.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sản phẩm chỉ được gắn vào danh mục con; danh mục gốc (có danh mục con) chỉ dùng để nhóm.
 */
@ExtendWith(MockitoExtension.class)
class ProductWriterCategoryRuleTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductWriter productWriter;

    private static Category root() {
        Category root = Category.builder().id(1L).code("vlxd").name("VẬT LIỆU XÂY DỰNG").build();
        Category child = Category.builder().id(2L).code("chau-rua").name("Chậu Rửa - Lavabo").parent(root).build();
        root.getChildren().add(child);
        return root;
    }

    private static Category leaf() {
        return Category.builder().id(2L).code("chau-rua").name("Chậu Rửa - Lavabo").children(new ArrayList<>()).build();
    }

    private static ProductCreateDto createDto(long categoryId) {
        return ProductCreateDto.builder().code("PRD-1").name("Chậu rửa").categoryId(categoryId).baseUnit("Cái").build();
    }

    @Test
    void createProduct_withRootCategory_isRejected() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productWriter.createProduct(createDto(1L)));

        assertEquals("Danh mục 'VẬT LIỆU XÂY DỰNG' là danh mục nhóm, hãy chọn một danh mục con", ex.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_withLeafCategory_isSaved() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(leaf()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = productWriter.createProduct(createDto(2L));

        assertEquals(2L, result.getCategoryId());
        assertEquals("Chậu Rửa - Lavabo", result.getCategoryName());
    }

    @Test
    void updateProduct_movingToRootCategory_isRejected() {
        Product existing = Product.builder().id(10L).code("PRD-1").name("Chậu rửa").category(leaf()).baseUnit("Cái").build();
        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root()));

        ProductUpdateDto dto = new ProductUpdateDto();
        dto.setCategoryId(1L);

        assertThrows(IllegalArgumentException.class, () -> productWriter.updateProduct(10L, dto));
        verify(productRepository, never()).save(any());
    }

    @Test
    void rootCategoryFixture_hasChild() {
        // Bảo đảm fixture đúng: @Builder.Default khởi tạo danh sách con.
        assertEquals(List.of("chau-rua"), root().getChildren().stream().map(Category::getCode).toList());
    }
}
