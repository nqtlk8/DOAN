package com.storename.erp.identity.application;

import com.storename.erp.common.security.JwtTokenProvider;
import com.storename.erp.common.security.TokenSessionValidator;
import com.storename.erp.identity.api.dto.AuthResponse;
import com.storename.erp.identity.domain.UserAccount;
import com.storename.erp.identity.domain.UserBranchRole;
import com.storename.erp.identity.domain.exception.NoRoleAssignedException;
import com.storename.erp.identity.infrastructure.UserRepository;
import com.storename.erp.identity.infrastructure.UserBranchRoleRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Dang nhap, gia han va thu hoi phien. Chi chay o HQ (va ALL khi dev) vi chi HQ giu private key.
 *
 * <p>Vong doi token:
 * <ul>
 *   <li><b>login</b>: kiem tra mat khau (BCrypt), trang thai, vai tro; cap access (30 phut) va refresh (7 ngay)
 *       mang claim tv = user_account.token_version.</li>
 *   <li><b>refresh</b>: doc lai user tu DB (con hoat dong, tv khop), lay lai vai tro tu DB, xoay vong refresh token.
 *       Idempotent: cac request trung refresh token trong khoang grace nhan lai dung cap token da cap;
 *       dung lai sau khoang grace bi coi la dau hieu lo token -> thu hoi moi phien cua user.</li>
 *   <li><b>revoke</b> (dang xuat): tang token_version -> moi token cua user het hieu luc tren moi thiet bi.</li>
 * </ul>
 */
@Slf4j
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'HQ' or '${instance.role:ALL}' == 'ALL'")
public class AuthService {

    private static final String REVOKED_TOKEN_PREFIX = "revoked_token:";
    private static final String REFRESH_USED_PREFIX = "refresh_used:";
    private static final String REFRESH_RESULT_PREFIX = "refresh_result:";
    private static final String FIELD_SEPARATOR = "\n";

    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final UserBranchRoleRepository userBranchRoleRepository;
    private final PasswordEncoder passwordEncoder;

    private TokenSessionValidator sessionValidator;

    /** Khoang thoi gian mot refresh token da dung van duoc tra lai ket qua cu (nhieu tab, retry mang). */
    @Value("${security.refresh-reuse-grace-ms:30000}")
    private long refreshReuseGraceMs = 30000;

    public AuthService(JwtTokenProvider tokenProvider, StringRedisTemplate redisTemplate,
                       UserRepository userRepository, UserBranchRoleRepository userBranchRoleRepository,
                       PasswordEncoder passwordEncoder) {
        this.tokenProvider = tokenProvider;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.userBranchRoleRepository = userBranchRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired(required = false)
    public void setSessionValidator(TokenSessionValidator sessionValidator) {
        this.sessionValidator = sessionValidator;
    }

    public AuthResponse login(String username, String password) {
        log.info("Attempting login for user: {}", username);
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            log.warn("Invalid credentials format for user: {}", username);
            throw new IllegalArgumentException("Invalid credentials format");
        }

        UserAccount user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new BadCredentialsException("Bad credentials");
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Password mismatch for user: {}", username);
            throw new BadCredentialsException("Bad credentials");
        }

        if (!user.isActive()) {
            log.warn("User is inactive: {}", username);
            throw new DisabledException("User is inactive");
        }

        AuthResponse response = issueTokens(user);
        log.info("Login successful for user: {}", username);
        return response;
    }

    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new JwtException("Refresh token is required");
        }
        Claims claims = tokenProvider.getClaimsFromToken(refreshToken);
        requireRefreshType(claims);

        String tokenId = claims.get("tokenId", String.class);
        if (tokenId == null || tokenId.isBlank()) {
            throw new JwtException("Refresh token has no tokenId");
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_TOKEN_PREFIX + tokenId))) {
            log.warn("Attempt to use revoked refresh token: {}", tokenId);
            throw new JwtException("Token has been revoked");
        }

        UserAccount user = findUserBySubject(claims.getSubject())
                .orElseThrow(() -> new JwtException("User of refresh token no longer exists"));
        if (!user.isActive()) {
            log.warn("Refresh rejected: user {} is inactive", user.getUsername());
            throw new JwtException("User is inactive");
        }
        if (user.getTokenVersion() != JwtTokenProvider.getTokenVersion(claims)) {
            log.info("Refresh rejected: token version mismatch for user {}", user.getUsername());
            throw new JwtException("Session is no longer valid");
        }

        long remainingMs = Math.max(claims.getExpiration().getTime() - System.currentTimeMillis(), 1000L);
        Boolean firstUse = redisTemplate.opsForValue()
                .setIfAbsent(REFRESH_USED_PREFIX + tokenId, "1", remainingMs, TimeUnit.MILLISECONDS);

        if (Boolean.TRUE.equals(firstUse)) {
            try {
                AuthResponse issued = issueTokens(user);
                redisTemplate.opsForValue().set(REFRESH_RESULT_PREFIX + tokenId, serialize(issued),
                        refreshReuseGraceMs, TimeUnit.MILLISECONDS);
                log.info("Refresh successful for user: {}", user.getUsername());
                return issued;
            } catch (RuntimeException e) {
                // Cap token that bai (vd. user mat vai tro) -> go danh dau de lan thu lai khong bi coi la dung lai token.
                redisTemplate.delete(REFRESH_USED_PREFIX + tokenId);
                throw e;
            }
        }

        // Refresh token nay da duoc dung: request trung (nhieu tab / retry) -> tra lai ket qua da cap.
        String previous = waitForPreviousResult(tokenId);
        if (previous != null) {
            log.info("Duplicate refresh within grace period for user {}: returning previously issued tokens", user.getUsername());
            return deserialize(previous);
        }

        // Dung lai sau khoang grace: coi nhu token bi lo -> thu hoi moi phien cua user.
        log.warn("Refresh token reuse detected for user {} (tokenId {}): revoking all sessions", user.getUsername(), tokenId);
        revokeAllSessions(user);
        throw new JwtException("Refresh token reuse detected. Please log in again.");
    }

    /**
     * Dang xuat: tang token_version cua user -> moi access/refresh token cua user tren moi thiet bi
     * het hieu luc (o HQ ngay lap tuc, o chi nhanh sau TTL cache + do tre replication).
     */
    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new JwtException("Refresh token is required");
        }
        Claims claims;
        try {
            claims = tokenProvider.getClaimsFromToken(refreshToken);
        } catch (ExpiredJwtException e) {
            // Token het han van xac dinh duoc user (chu ky da hop le) -> van cho dang xuat.
            claims = e.getClaims();
        }
        requireRefreshType(claims);

        findUserBySubject(claims.getSubject()).ifPresent(this::revokeAllSessions);

        String tokenId = claims.get("tokenId", String.class);
        Date expiration = claims.getExpiration();
        long ttl = expiration != null ? expiration.getTime() - System.currentTimeMillis() : 0;
        if (tokenId != null && ttl > 0) {
            redisTemplate.opsForValue().set(REVOKED_TOKEN_PREFIX + tokenId, "true", ttl, TimeUnit.MILLISECONDS);
        }
        log.info("Logout: sessions revoked for subject {}", claims.getSubject());
    }

    private AuthResponse issueTokens(UserAccount user) {
        UserBranchRole primaryRole = primaryRole(user);
        String roleCode = primaryRole.getRole().getCode();
        String branchIdStr = primaryRole.getBranch() != null ? primaryRole.getBranch().getId().toString() : null;
        // branchUrl = dia chi cong web cua chi nhanh (cot branch.internal_url); frontend dung de buoc
        // STAFF lam viec dung cong chi nhanh cua minh. Day la URL cong khai, khong phai dia chi backend.
        String branchUrl = primaryRole.getBranch() != null ? primaryRole.getBranch().getInternalUrl() : null;

        String subject = user.getPublicId().toString();
        int tokenVersion = user.getTokenVersion();
        String accessToken = tokenProvider.generateToken(subject, roleCode, branchIdStr,
                UUID.randomUUID().toString(), tokenVersion);
        String refreshToken = tokenProvider.generateRefreshToken(subject, roleCode, branchIdStr,
                UUID.randomUUID().toString(), tokenVersion);
        return new AuthResponse(accessToken, refreshToken, roleCode, branchUrl);
    }

    /** Thiet ke: moi user chi thuoc mot chi nhanh. Neu co nhieu vai tro, dung dong co id nho nhat. */
    private UserBranchRole primaryRole(UserAccount user) {
        List<UserBranchRole> roles = userBranchRoleRepository.findByUserId(user.getId());
        if (roles.isEmpty()) {
            log.warn("User has no roles assigned: {}", user.getUsername());
            throw new NoRoleAssignedException("User has no roles assigned");
        }
        if (roles.size() > 1) {
            log.warn("User {} has {} role assignments; using the first one (id order). Each user should belong to one branch.",
                    user.getUsername(), roles.size());
        }
        return roles.get(0);
    }

    private void revokeAllSessions(UserAccount user) {
        userRepository.incrementTokenVersion(user.getId());
        if (sessionValidator != null) {
            sessionValidator.evict(user.getPublicId().toString());
        }
    }

    private void requireRefreshType(Claims claims) {
        if (!"refresh".equals(claims.get("type", String.class))) {
            log.warn("Expected a refresh token");
            throw new JwtException("Invalid token type. Expected refresh token.");
        }
    }

    private Optional<UserAccount> findUserBySubject(String subject) {
        try {
            return userRepository.findByPublicId(UUID.fromString(subject));
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Rejecting refresh token with non-UUID subject");
            throw new JwtException("Invalid token subject. Please log in again.");
        }
    }

    private String waitForPreviousResult(String tokenId) {
        // Request dau tien co the van dang sinh token: cho toi da ~500ms.
        for (int i = 0; i < 5; i++) {
            String value = redisTemplate.opsForValue().get(REFRESH_RESULT_PREFIX + tokenId);
            if (value != null) {
                return value;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private static String serialize(AuthResponse r) {
        return String.join(FIELD_SEPARATOR, r.getAccessToken(), r.getRefreshToken(),
                r.getRole() == null ? "" : r.getRole(), r.getBranchUrl() == null ? "" : r.getBranchUrl());
    }

    private static AuthResponse deserialize(String value) {
        String[] p = value.split(FIELD_SEPARATOR, -1);
        return new AuthResponse(p[0], p[1], p[2].isEmpty() ? null : p[2],
                p.length > 3 && !p[3].isEmpty() ? p[3] : null);
    }
}
