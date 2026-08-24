package com.storename.erp.catalog.domain;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "category_attribute")
@Getter

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryAttribute {

    @EmbeddedId
    private CategoryAttributeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attributeId")
    @JoinColumn(name = "attribute_id")
    private AttributeDefinition attribute;

    @Embeddable
    @Getter
    
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryAttributeId implements Serializable {
        private Long categoryId;
        private Short attributeId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CategoryAttributeId that = (CategoryAttributeId) o;
            return Objects.equals(categoryId, that.categoryId) && Objects.equals(attributeId, that.attributeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(categoryId, attributeId);
        }
    }
}
