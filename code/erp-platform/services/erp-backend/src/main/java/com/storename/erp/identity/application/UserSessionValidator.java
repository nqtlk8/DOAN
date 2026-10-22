package com.storename.erp.identity.application;

import com.storename.erp.common.security.TokenSessionValidator;
import com.storename.erp.identity.domain.UserAccount;
import com.storename.erp.identity.infrastructure.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kiem tra access token theo trang thai user trong DB (chay o ca HQ va Branch,
 * vi bang user_account duoc replicate HQ -> Branch).
 * <p>Ket qua tra cuu duoc cache ngan han (mac dinh 30 giay) de khong truy van DB o moi request;
 * vi vay dang xuat / khoa tai khoan co hieu luc toi da sau TTL cache + do tre replication.
 * Tat bang {@code security.session-validation.enabled=false} (chi dung cho test dung token gia).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "security.session-validation.enabled", havingValue = "true", matchIfMissing = true)
public class UserSessionValidator implements TokenSessionValidator {

    private static final int MAX_ENTRIES = 10_000;

    private record CachedState(boolean active, int tokenVersion, long expiresAt) {}

    private final UserRepository userRepository;
    private final long cacheTtlMs;
    private final ConcurrentHashMap<UUID, CachedState> cache = new ConcurrentHashMap<>();

    public UserSessionValidator(UserRepository userRepository,
                                @Value("${security.session-cache-ttl-ms:30000}") long cacheTtlMs) {
        this.userRepository = userRepository;
        this.cacheTtlMs = cacheTtlMs;
    }

    @Override
    public boolean isSessionValid(String subject, int tokenVersion) {
        UUID publicId = parse(subject);
        if (publicId == null) {
            return false;
        }
        CachedState state = load(publicId);
        if (state == null) {
            log.warn("Rejecting token: user {} not found", publicId);
            return false;
        }
        if (!state.active()) {
            log.warn("Rejecting token: user {} is inactive", publicId);
            return false;
        }
        if (state.tokenVersion() != tokenVersion) {
            log.info("Rejecting token: user {} token version {} != current {}", publicId, tokenVersion, state.tokenVersion());
            return false;
        }
        return true;
    }

    @Override
    public void evict(String subject) {
        UUID publicId = parse(subject);
        if (publicId != null) {
            cache.remove(publicId);
        }
    }

    private CachedState load(UUID publicId) {
        long now = System.currentTimeMillis();
        CachedState cached = cache.get(publicId);
        if (cached != null && cached.expiresAt() > now) {
            return cached;
        }
        Optional<UserAccount> user = userRepository.findByPublicId(publicId);
        if (user.isEmpty()) {
            cache.remove(publicId);
            return null;
        }
        if (cache.size() >= MAX_ENTRIES) {
            cache.clear();
        }
        CachedState fresh = new CachedState(user.get().isActive(), user.get().getTokenVersion(), now + cacheTtlMs);
        cache.put(publicId, fresh);
        return fresh;
    }

    private static UUID parse(String subject) {
        if (subject == null) {
            return null;
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
