package com.storename.erp.identity.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Dinh danh cong khai (UUID) cua user, dung lam JWT subject va gia tri cho cac cot
     * audit kieu UUID (created_by, confirmed_by...). Khong dung id BIGINT noi bo ra ngoai.
     */
    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(length = 200)
    private String email;

    @Column(length = 20)
    private String phone;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Phien ban phien dang nhap. Moi token mang claim tv = gia tri nay luc phat hanh;
     * tang len 1 khi dang xuat hoac phat hien refresh token bi dung lai -> moi token cu het hieu luc
     * (ca o chi nhanh, vi bang user_account duoc replicate HQ -> Branch).
     */
    @Builder.Default
    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
