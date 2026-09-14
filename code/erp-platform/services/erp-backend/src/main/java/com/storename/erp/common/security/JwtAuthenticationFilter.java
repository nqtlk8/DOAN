package com.storename.erp.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.common.api.ApiResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                Claims claims = tokenProvider.getClaimsFromToken(jwt);
                
                String type = claims.get("type", String.class);
                if (!"access".equals(type)) {
                    throw new io.jsonwebtoken.JwtException("Invalid token type. Expected access token.");
                }

                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                
                if (role == null || role.trim().isEmpty()) {
                    throw new io.jsonwebtoken.JwtException("Token missing required 'role' claim");
                }

                String branchId = claims.get("branchId", String.class);
                String tokenId = claims.get("tokenId", String.class);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.singletonList(new SimpleGrantedAuthority(role)));
                
                authentication.setDetails(new JwtAuthDetails(branchId, tokenId));
                
                request.setAttribute("branchId", branchId);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (io.jsonwebtoken.JwtException ex) {
            logger.error("Invalid JWT token", ex);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            ApiResponse<Void> apiResponse = ApiResponse.error("Unauthorized: " + ex.getMessage(), null);
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            ApiResponse<Void> apiResponse = ApiResponse.error("Internal Server Error during authentication", null);
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
