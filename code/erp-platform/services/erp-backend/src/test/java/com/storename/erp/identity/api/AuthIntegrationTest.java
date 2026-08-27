package com.storename.erp.identity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.identity.api.dto.AuthRequest;
import com.storename.erp.identity.api.dto.AuthResponse;
import com.storename.erp.identity.api.dto.TokenRefreshRequest;
import com.storename.erp.identity.domain.Role;
import com.storename.erp.identity.domain.UserAccount;
import com.storename.erp.identity.domain.UserBranchRole;
import com.storename.erp.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.junit.jupiter.api.BeforeEach;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.type.TypeReference;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "instance.role=HQ",
    "spring.datasource.url=jdbc:h2:mem:auth_test_db_${random.uuid};DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private StringRedisTemplate redisTemplate;
    
    @MockitoBean
    private com.storename.erp.branch.infrastructure.BranchRepository branchRepository;
    
    @MockitoBean
    private com.storename.erp.identity.infrastructure.UserRepository userRepository;
    
    @MockitoBean
    private com.storename.erp.identity.infrastructure.UserBranchRoleRepository userBranchRoleRepository;

    @org.mockito.Mock
    private ValueOperations<String, String> valueOperations;

    private Map<String, String> fakeRedis = new HashMap<>();

    @BeforeEach
    public void setup() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            fakeRedis.put(key, value);
            return null;
        }).when(valueOperations).set(anyString(), anyString(), anyLong(), any());

        when(redisTemplate.hasKey(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return fakeRedis.containsKey(key);
        });

        // Setup mock user ADMIN
        UserAccount mockUser = new UserAccount();
        mockUser.setId(1L);
        mockUser.setUsername("admin");
        mockUser.setPasswordHash(passwordEncoder.encode("password"));
        mockUser.setActive(true);

        Role mockRole = new Role();
        mockRole.setCode("ADMIN");
        
        UserBranchRole ubr = new UserBranchRole();
        ubr.setUser(mockUser);
        ubr.setRole(mockRole);
        
        List<UserBranchRole> roles = new ArrayList<>();
        roles.add(ubr);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(mockUser));
        when(userBranchRoleRepository.findByUserId(1L)).thenReturn(roles);
    }

    @Test
    public void testLoginAdminSuccess() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("admin");
        authRequest.setPassword("password");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.branchUrl").isEmpty());
    }

    @Test
    public void testLoginStaffSuccess() throws Exception {
        // Setup mock user for STAFF
        UserAccount staffUser = new UserAccount();
        staffUser.setId(2L);
        staffUser.setUsername("staff");
        staffUser.setPasswordHash(passwordEncoder.encode("password"));
        staffUser.setActive(true);

        Role staffRole = new Role();
        staffRole.setCode("STAFF");
        
        com.storename.erp.branch.domain.Branch branch = new com.storename.erp.branch.domain.Branch();
        branch.setId(1L);
        branch.setInternalUrl("http://branch-tp1.local");
        
        UserBranchRole ubr = new UserBranchRole();
        ubr.setUser(staffUser);
        ubr.setRole(staffRole);
        ubr.setBranch(branch);
        
        List<UserBranchRole> roles = new ArrayList<>();
        roles.add(ubr);

        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staffUser));
        when(userBranchRoleRepository.findByUserId(2L)).thenReturn(roles);

        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("staff");
        authRequest.setPassword("password");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.role").value("STAFF"))
                .andExpect(jsonPath("$.data.branchUrl").value("http://branch-tp1.local"));
    }

    @Test
    public void testLoginFailure_BadCredentials() throws Exception {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setUsername("admin");
        authRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bad credentials"));
    }
}
