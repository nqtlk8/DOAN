package com.storename.erp.catalog.infrastructure;

import com.storename.erp.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByCode(String code);

    /** Danh mục đang hoạt động kèm danh mục cha (fetch join, tránh N+1): gốc trước, rồi theo id. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT c FROM Category c LEFT JOIN FETCH c.parent p WHERE c.isActive = true "
                    + "ORDER BY CASE WHEN p IS NULL THEN 0 ELSE 1 END, c.id")
    java.util.List<Category> findAllActiveWithParent();
}
