package com.store.erp.catalog.repos;

import com.store.erp.catalog.entity.Distributor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DistributorRepository extends JpaRepository<Distributor, UUID> {
    @Query("SELECT d FROM Distributor d WHERE " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(d.contactInfo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(d.metadata AS string) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Distributor> search(@Param("search") String search, Pageable pageable);
}

