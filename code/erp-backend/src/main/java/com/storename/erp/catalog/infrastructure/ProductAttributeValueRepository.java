package com.storename.erp.catalog.infrastructure;

import com.storename.erp.catalog.domain.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, ProductAttributeValue.ProductAttributeValueId> {
    List<ProductAttributeValue> findByProductId(Long productId);
}
