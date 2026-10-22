package com.storename.erp.identity.infrastructure;

import com.storename.erp.identity.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByPublicId(UUID publicId);

    /** Tang token_version nguyen tu (dang xuat moi thiet bi / thu hoi phien). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserAccount u set u.tokenVersion = u.tokenVersion + 1 where u.id = :id")
    @org.springframework.transaction.annotation.Transactional
    int incrementTokenVersion(@Param("id") Long id);
}
