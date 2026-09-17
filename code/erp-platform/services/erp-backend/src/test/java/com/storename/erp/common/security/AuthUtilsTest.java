package com.storename.erp.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** BUG-1: user id lay tu JWT subject phai la UUID; subject so ("2") phai bi tu choi ro rang. */
class AuthUtilsTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String subject) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                subject, null, List.of(new SimpleGrantedAuthority("STAFF")));
        auth.setDetails(new JwtAuthDetails("1", "token-1"));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getUserId_returnsUuid_whenSubjectIsUuid() {
        UUID id = UUID.randomUUID();
        authenticateAs(id.toString());
        assertEquals(id, AuthUtils.getUserId());
    }

    @Test
    void getUserId_throwsAccessDenied_whenSubjectIsLegacyNumericId() {
        authenticateAs("2");
        assertNull(AuthUtils.getUserIdOrNull());
        assertThrows(AccessDeniedException.class, AuthUtils::getUserId);
    }

    @Test
    void getUserId_throwsAccessDenied_whenNotAuthenticated() {
        assertNull(AuthUtils.getUserIdOrNull());
        assertThrows(AccessDeniedException.class, AuthUtils::getUserId);
    }
}
