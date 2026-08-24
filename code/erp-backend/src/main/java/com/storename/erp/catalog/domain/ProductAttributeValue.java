package com.storename.erp.catalog.domain;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "product_attribute_value")
@Getter

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttributeValue {

    @EmbeddedId
    private ProductAttributeValueId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attributeId")
    @JoinColumn(name = "attribute_id")
    private AttributeDefinition attribute;

    @Column(name = "value_text", length = 500)
    private String valueText;

    @Column(name = "value_number", precision = 19, scale = 4)
    private BigDecimal valueNumber;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductAttributeValueId implements Serializable {
        private Long productId;
        private Short attributeId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProductAttributeValueId that = (ProductAttributeValueId) o;
            return Objects.equals(productId, that.productId) && Objects.equals(attributeId, that.attributeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, attributeId);
        }
    }

    public void updateValueText(String value) {
        this.valueText = value;
    }

    public void updateValueNumber(BigDecimal value) {
        this.valueNumber = value;
    }

    public void updateValueBoolean(Boolean value) {
        this.valueBoolean = value;
    }
}
