package com.storename.erp.catalog;

import com.storename.erp.catalog.application.ProductReader;
import com.storename.erp.catalog.application.ProductWriter;
import com.storename.erp.catalog.application.dto.ProductCreateDto;
import com.storename.erp.catalog.application.dto.ProductResponseDto;
import com.storename.erp.catalog.application.dto.ProductUpdateDto;
import com.storename.erp.catalog.domain.AttributeDefinition;
import com.storename.erp.catalog.domain.Category;
import com.storename.erp.catalog.infrastructure.AttributeDefinitionRepository;
import com.storename.erp.catalog.infrastructure.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("hq")
@Transactional
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:hq_catalog_test_db",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
public class ProductServiceIntegrationTest {

    @Autowired
    private ProductWriter productWriter;

    @Autowired
    private ProductReader productReader;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AttributeDefinitionRepository attributeDefinitionRepository;

    private Category testCategory;
    private AttributeDefinition testAttribute;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.save(Category.builder()
                .code("CAT01")
                .name("Electronics")
                .isActive(true)
                .build());

        testAttribute = attributeDefinitionRepository.save(AttributeDefinition.builder()
                .code("COLOR")
                .name("Color")
                .dataType("TEXT")
                .build());
    }

    @Test
    void testCreateAndReadProduct() {
        // Arrange
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("COLOR", "Red");

        ProductCreateDto createDto = ProductCreateDto.builder()
                .code("PROD01")
                .name("Test Product")
                .categoryId(testCategory.getId())
                .baseUnit("PCS")
                .isActive(true)
                .attributes(attributes)
                .build();

        // Act
        ProductResponseDto created = productWriter.createProduct(createDto);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getCode()).isEqualTo("PROD01");
        assertThat(created.getAttributes()).containsEntry("COLOR", "Red");

        // Read Test
        ProductResponseDto readProduct = productReader.getProductById(created.getId());
        assertThat(readProduct).isNotNull();
        assertThat(readProduct.getCode()).isEqualTo("PROD01");
        
        List<ProductResponseDto> allProducts = productReader.getAllProducts();
        assertThat(allProducts).hasSize(1);
    }

    @Test
    void testUpdateProduct() {
        // Arrange
        ProductCreateDto createDto = ProductCreateDto.builder()
                .code("PROD02")
                .name("Initial Product")
                .categoryId(testCategory.getId())
                .baseUnit("PCS")
                .isActive(true)
                .build();
        ProductResponseDto created = productWriter.createProduct(createDto);

        Map<String, Object> newAttributes = new HashMap<>();
        newAttributes.put("COLOR", "Blue");

        ProductUpdateDto updateDto = ProductUpdateDto.builder()
                .name("Updated Product")
                .isActive(false)
                .attributes(newAttributes)
                .build();

        // Act
        ProductResponseDto updated = productWriter.updateProduct(created.getId(), updateDto);

        // Assert
        assertThat(updated.getName()).isEqualTo("Updated Product");
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getAttributes()).containsEntry("COLOR", "Blue");
    }

    @Autowired
    private com.storename.erp.catalog.application.CategoryWriter categoryWriter;

    @Test
    void testDeleteCategoryWithConstraints() {
        // 1. Cannot delete category with child
        Category childCategory = categoryRepository.save(Category.builder()
                .code("CAT02")
                .name("Smartphones")
                .parent(testCategory)
                .isActive(true)
                .build());
                
        // Manually update the parent collection to reflect the relationship in-memory if needed
        testCategory.getChildren().add(childCategory);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> categoryWriter.deleteCategory(testCategory.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete category because it has child categories");

        // Cleanup child to test product constraint
        testCategory.getChildren().remove(childCategory);
        categoryRepository.delete(childCategory);

        // 2. Cannot delete category with product
        ProductCreateDto createDto = ProductCreateDto.builder()
                .code("PROD03")
                .name("Test Phone")
                .categoryId(testCategory.getId())
                .baseUnit("PCS")
                .isActive(true)
                .build();
        productWriter.createProduct(createDto);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> categoryWriter.deleteCategory(testCategory.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete category because it has");
    }
}
