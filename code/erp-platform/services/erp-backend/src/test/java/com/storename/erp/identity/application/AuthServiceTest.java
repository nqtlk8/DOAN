package com.storename.erp.identity.application;

import com.storename.erp.common.security.JwtTokenProvider;
import com.storename.erp.identity.domain.UserAccount;
import com.storename.erp.identity.infrastructure.UserRepository;
import com.storename.erp.identity.infrastructure.UserBranchRoleRepository;
import com.storename.erp.identity.domain.exception.NoRoleAssignedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserBranchRoleRepository userBranchRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private UserAccount mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new UserAccount();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");
        mockUser.setPasswordHash("encoded_password");
        mockUser.setActive(true);
    }

    @Test
    void login_whenUserHasNoRoles_throwsNoRoleAssignedException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password", "encoded_password")).thenReturn(true);
        when(userBranchRoleRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        assertThrows(NoRoleAssignedException.class, () -> authService.login("testuser", "password"));
    }

    @Test
    void login_shouldUsePublicIdUuidAsJwtSubject_notNumericId() {
        // BUG-1: subject phai la UUID (public_id), khong phai id BIGINT ("1")
        java.util.UUID publicId = java.util.UUID.randomUUID();
        mockUser.setPublicId(publicId);

        com.storename.erp.identity.domain.Role role = mock(com.storename.erp.identity.domain.Role.class);
        when(role.getCode()).thenReturn("STAFF");
        com.storename.erp.identity.domain.UserBranchRole ubr = mock(com.storename.erp.identity.domain.UserBranchRole.class);
        when(ubr.getRole()).thenReturn(role);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password", "encoded_password")).thenReturn(true);
        when(userBranchRoleRepository.findByUserId(1L)).thenReturn(java.util.List.of(ubr));
        when(tokenProvider.generateToken(anyString(), anyString(), any(), anyString())).thenReturn("access");
        when(tokenProvider.generateRefreshToken(anyString(), anyString(), any(), anyString())).thenReturn("refresh");

        authService.login("testuser", "password");

        verify(tokenProvider).generateToken(eq(publicId.toString()), eq("STAFF"), any(), anyString());
        verify(tokenProvider).generateRefreshToken(eq(publicId.toString()), eq("STAFF"), any(), anyString());
    }
}
