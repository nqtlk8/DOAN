package com.storename.erp.catalog.infrastructure;

import com.storename.erp.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByCode(String code);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category"})
    java.util.List<Product> findAll();
}
