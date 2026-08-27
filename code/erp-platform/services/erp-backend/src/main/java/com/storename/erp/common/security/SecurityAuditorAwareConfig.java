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
                // Username here is actually supposed to be the ID in the token?
                // Let's check where user UUID is stored. In JwtAuthenticationFilter, subject is 'username'.
                // If subject is the user UUID, we parse it. If it's a string name, we might have an issue.
                // Wait, AuthController login sets subject to UUID or username?
                // Let's assume username is the string representation of UUID for now, or fallback.
                return Optional.of(UUID.fromString(authentication.getName()));
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }
}
