package com.storename.erp.identity.infrastructure;

import com.storename.erp.identity.domain.UserBranchRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBranchRoleRepository extends JpaRepository<UserBranchRole, Long> {
    List<UserBranchRole> findByUserId(Long userId);
}
