package com.storename.erp.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.common.api.ApiResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Xac thuc access token tren moi request.
 * <ol>
 *   <li>Bo qua cac duong dan cong khai (auth, public, health, swagger): token cu/het han
 *       gui kem khong lam hong request cong khai.</li>
 *   <li>Xac minh chu ky va han bang public key, bat buoc {@code type = access} va co claim {@code role}.</li>
 *   <li>Server chi nhanh: token co branchId phai trung chi nhanh cua server; token khong co
 *       branchId (ADMIN toan he thong) duoc gan chi nhanh cua server.</li>
 *   <li>Kiem tra phien theo DB qua {@link TokenSessionValidator}: user con ton tai, dang hoat dong
 *       va token_version khop (dang xuat / khoa tai khoan co hieu luc ca o chi nhanh).</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/v1/auth/", "/api/v1/public/", "/api/health", "/error", "/swagger-ui", "/v3/api-docs");

    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    /** Null trong unit test dung constructor truc tiep; khi do bo qua kiem tra phien theo DB. */
    private TokenSessionValidator sessionValidator;

    @Value("${instance.role:ALL}")
    private String instanceRole;

    // Id SO cua branch (khop claim branchId trong JWT = branch.id), lay tu env BRANCH_ID
    // (relaxed binding cua key "branch-id") hoac instance.branch-id.
    @Value("${branch-id:${instance.branch-id:}}")
    private String configuredBranchId;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
    }

    @Autowired(required = false)
    public void setSessionValidator(TokenSessionValidator sessionValidator) {
        this.sessionValidator = sessionValidator;
    }

    /** Bao loi ro rang luc khoi dong neu server chi nhanh thieu hoac sai BRANCH_ID. */
    @PostConstruct
    void validateBranchConfig() {
        if (!"BRANCH".equalsIgnoreCase(instanceRole)) {
            return;
        }
        if (!StringUtils.hasText(configuredBranchId)) {
            logger.error("instance.role=BRANCH but BRANCH_ID (branch-id) is not set: every branch token will be rejected (403).");
            return;
        }
        try {
            Long.parseLong(configuredBranchId.trim());
        } catch (NumberFormatException e) {
            logger.error("BRANCH_ID '" + configuredBranchId + "' is not the numeric id of table branch: branch tokens will be rejected (403).");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
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

                String subject = claims.getSubject();
                String role = claims.get("role", String.class);

                if (role == null || role.trim().isEmpty()) {
                    throw new io.jsonwebtoken.JwtException("Token missing required 'role' claim");
                }

                String branchId = claims.get("branchId", String.class);

                if ("BRANCH".equalsIgnoreCase(instanceRole)) {
                    if (branchId != null && !branchId.equals(configuredBranchId)) {
                        logger.warn(String.format("Branch isolation violation: token branchId %s does not match configured branchId %s", branchId, configuredBranchId));
                        writeError(response, HttpServletResponse.SC_FORBIDDEN, "Branch isolation violation", "FORBIDDEN_BRANCH");
                        return;
                    }
                    if (branchId == null && StringUtils.hasText(configuredBranchId)) {
                        // ADMIN toan he thong lam viec tren server chi nhanh: dung chi nhanh cua server.
                        branchId = configuredBranchId.trim();
                    }
                }

                if (sessionValidator != null
                        && !sessionValidator.isSessionValid(subject, JwtTokenProvider.getTokenVersion(claims))) {
                    throw new io.jsonwebtoken.JwtException("Session is no longer valid (logged out or account disabled)");
                }

                String tokenId = claims.get("tokenId", String.class);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        subject, null, Collections.singletonList(new SimpleGrantedAuthority(role)));

                authentication.setDetails(new JwtAuthDetails(branchId, tokenId));

                request.setAttribute("branchId", branchId);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            logger.debug("Expired JWT token");
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: token expired", "TOKEN_EXPIRED");
        } catch (io.jsonwebtoken.JwtException ex) {
            // Chi ghi chi tiet vao log; client nhan thong diep chung.
            logger.warn("Rejected JWT token: " + ex.getMessage());
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: invalid or revoked token", "INVALID_TOKEN");
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error during authentication", "INTERNAL_ERROR");
        }
    }

    private void writeError(HttpServletResponse response, int status, String message, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> apiResponse = ApiResponse.error(message, Collections.singletonList(code));
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
