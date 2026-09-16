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
}
