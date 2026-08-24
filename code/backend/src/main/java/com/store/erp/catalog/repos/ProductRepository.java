package com.store.erp.catalog.repos;

import com.store.erp.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Query("SELECT p FROM Product p WHERE (:code IS NULL OR p.code LIKE %:code%) AND (:name IS NULL OR p.name LIKE %:name%)")
    Page<Product> searchProducts(@Param("code") String code, @Param("name") String name, Pageable pageable);
}

