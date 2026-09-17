package com.storename.erp.identity.application;

import com.storename.erp.common.security.JwtTokenProvider;
import com.storename.erp.identity.api.dto.AuthResponse;
import com.storename.erp.identity.domain.UserAccount;
import com.storename.erp.identity.domain.UserBranchRole;
import com.storename.erp.identity.infrastructure.UserRepository;
import com.storename.erp.identity.infrastructure.UserBranchRoleRepository;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'HQ' or '${instance.role:ALL}' == 'ALL'")
public class AuthService {

    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final UserBranchRoleRepository userBranchRoleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String REVOKED_TOKEN_PREFIX = "revoked_token:";

    public AuthService(JwtTokenProvider tokenProvider, StringRedisTemplate redisTemplate,
                       UserRepository userRepository, UserBranchRoleRepository userBranchRoleRepository,
                       PasswordEncoder passwordEncoder) {
        this.tokenProvider = tokenProvider;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.userBranchRoleRepository = userBranchRoleRepository;
        this.passwordEncoder = passwordEncoder;
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
                    return new org.springframework.security.authentication.BadCredentialsException("Bad credentials");
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Password mismatch for user: {}", username);
            throw new org.springframework.security.authentication.BadCredentialsException("Bad credentials");
        }

        if (!user.isActive()) {
            log.warn("User is inactive: {}", username);
            throw new org.springframework.security.authentication.DisabledException("User is inactive");
        }

        List<UserBranchRole> roles = userBranchRoleRepository.findByUserId(user.getId());
        if (roles.isEmpty()) {
            log.warn("User has no roles assigned: {}", username);
            throw new com.storename.erp.identity.domain.exception.NoRoleAssignedException("User has no roles assigned");
        }

        UserBranchRole primaryRole = roles.get(0);
        String roleCode = primaryRole.getRole().getCode();
        String branchIdStr = primaryRole.getBranch() != null ? primaryRole.getBranch().getId().toString() : null;
        String branchUrl = primaryRole.getBranch() != null ? primaryRole.getBranch().getInternalUrl() : null;
        
        String accessTokenId = UUID.randomUUID().toString();
        String refreshTokenId = UUID.randomUUID().toString();

        String accessToken = tokenProvider.generateToken(user.getId().toString(), roleCode, branchIdStr, accessTokenId);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId().toString(), roleCode, branchIdStr, refreshTokenId);

        log.info("Login successful for user: {}", username);
        return new AuthResponse(accessToken, refreshToken, roleCode, branchUrl);
    }

    public AuthResponse refresh(String refreshToken) {
        log.info("Attempting token refresh");
        Claims claims = tokenProvider.getClaimsFromToken(refreshToken);
        
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            log.warn("Attempt to refresh with non-refresh token");
            throw new io.jsonwebtoken.JwtException("Invalid token type. Expected refresh token.");
        }

        String tokenId = claims.get("tokenId", String.class);

        if (Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_TOKEN_PREFIX + tokenId))) {
            log.warn("Attempt to use revoked refresh token: {}", tokenId);
            throw new io.jsonwebtoken.JwtException("Token has been revoked");
        }

        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        String branchId = claims.get("branchId", String.class);
        
        String newAccessTokenId = UUID.randomUUID().toString();
        String newRefreshTokenId = UUID.randomUUID().toString();
        
        String newAccessToken = tokenProvider.generateToken(username, role, branchId, newAccessTokenId);
        String newRefreshToken = tokenProvider.generateRefreshToken(username, role, branchId, newRefreshTokenId);

        revoke(refreshToken);

        log.info("Refresh successful for user: {}", username);
        // Note: branchUrl is not strictly needed for refresh response as frontend already has it.
        return new AuthResponse(newAccessToken, newRefreshToken, role, null);
    }

    public void revoke(String refreshToken) {
        Claims claims = tokenProvider.getClaimsFromToken(refreshToken);
        String tokenId = claims.get("tokenId", String.class);
        Date expiration = claims.getExpiration();
        long ttl = expiration.getTime() - System.currentTimeMillis();

        if (ttl > 0) {
            redisTemplate.opsForValue().set(REVOKED_TOKEN_PREFIX + tokenId, "true", ttl, TimeUnit.MILLISECONDS);
            log.info("Token revoked successfully: {}", tokenId);
        }
    }
}
