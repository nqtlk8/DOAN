package com.storename.erp.identity.domain;

import com.storename.erp.branch.domain.Branch;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_branch_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBranchRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @PrePersist
    @PreUpdate
    public void validateRoleBranch() {
        if (role != null) {
            if ("STAFF".equals(role.getCode()) && branch == null) {
                throw new IllegalStateException("STAFF must have a branch");
            }
            if ("ADMIN".equals(role.getCode()) && branch != null) {
                throw new IllegalStateException("ADMIN cannot have a branch");
            }
        }
    }
}
