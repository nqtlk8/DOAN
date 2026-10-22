package com.storename.erp.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
public class SecurityAuditorAwareConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.empty();
            }
            try {
                // JWT subject la user_account.public_id (UUID), xem JwtTokenProvider.
                return Optional.of(UUID.fromString(authentication.getName()));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }
}
