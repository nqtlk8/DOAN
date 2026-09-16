package com.storename.erp.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(tokenProvider, new ObjectMapper());
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    private void configureFilter(String instanceRole, String configuredBranchId) {
        ReflectionTestUtils.setField(filter, "instanceRole", instanceRole);
        ReflectionTestUtils.setField(filter, "configuredBranchId", configuredBranchId);
    }

    private Claims createClaims(String role, String branchId) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "access");
        map.put("role", role);
        if (branchId != null) {
            map.put("branchId", branchId);
        }
        map.put("tokenId", "token-123");
        map.put("sub", "user1");
        
        Claims claims = new DefaultClaims(map);
        return claims;
    }

    @Test
    void testStaffValidBranch() throws Exception {
        configureFilter("BRANCH", "1");
        request.addHeader("Authorization", "Bearer valid-token");
        when(tokenProvider.getClaimsFromToken("valid-token")).thenReturn(createClaims("STAFF", "1"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testStaffInvalidBranch() throws Exception {
        configureFilter("BRANCH", "2");
        request.addHeader("Authorization", "Bearer invalid-token");
        when(tokenProvider.getClaimsFromToken("invalid-token")).thenReturn(createClaims("STAFF", "1"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("Branch isolation violation"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testAdminBranchNull() throws Exception {
        configureFilter("BRANCH", "1");
        request.addHeader("Authorization", "Bearer admin-token");
        when(tokenProvider.getClaimsFromToken("admin-token")).thenReturn(createClaims("ADMIN", null));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testNoToken() throws Exception {
        configureFilter("BRANCH", "1");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testHqRole() throws Exception {
        configureFilter("HQ", "1");
        request.addHeader("Authorization", "Bearer hq-token");
        when(tokenProvider.getClaimsFromToken("hq-token")).thenReturn(createClaims("STAFF", "2"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
