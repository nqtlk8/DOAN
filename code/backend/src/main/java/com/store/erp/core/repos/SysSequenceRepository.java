package com.store.erp.core.repos;

import com.store.erp.core.entity.SysSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysSequenceRepository extends JpaRepository<SysSequence, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SysSequence> findById(String seqKey);
}

