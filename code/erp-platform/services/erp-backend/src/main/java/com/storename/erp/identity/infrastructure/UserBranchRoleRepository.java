package com.storename.erp.identity.infrastructure;

import com.storename.erp.identity.domain.UserBranchRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBranchRoleRepository extends JpaRepository<UserBranchRole, Long> {

    /**
     * Vai tro cua user, sap xep on dinh theo id. Thiet ke: moi user chi thuoc mot chi nhanh,
     * nen phan tu dau tien la vai tro chinh; neu co nhieu dong, AuthService ghi canh bao.
     */
    @Query("select ubr from UserBranchRole ubr join fetch ubr.role left join fetch ubr.branch "
            + "where ubr.user.id = :userId order by ubr.id asc")
    List<UserBranchRole> findByUserId(@Param("userId") Long userId);
}
