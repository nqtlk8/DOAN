package com.storename.erp.catalog.infrastructure;

import com.storename.erp.catalog.domain.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Short> {
    Optional<AttributeDefinition> findByCode(String code);
}
