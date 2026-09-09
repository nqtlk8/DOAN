package com.storename.erp.identity.api;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.identity.api.dto.AuthRequest;
import com.storename.erp.identity.api.dto.AuthResponse;
import com.storename.erp.identity.api.dto.TokenRefreshRequest;
import com.storename.erp.identity.application.AuthService;
import com.storename.erp.common.api.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")
@Slf4j
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException e) {
            if ("Bad credentials".equals(e.getMessage()) || "Invalid credentials format".equals(e.getMessage()) || "User is inactive".equals(e.getMessage())) {
                return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage(), Collections.emptyList()));
            }
            throw e; 
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody TokenRefreshRequest request) {
        try {
            AuthResponse response = authService.refresh(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (io.jsonwebtoken.JwtException e) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid token", Collections.emptyList()));
        } catch (RuntimeException e) {
            if ("Token has been revoked".equals(e.getMessage()) || "Invalid token type. Expected refresh token.".equals(e.getMessage())) {
                return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage(), Collections.emptyList()));
            }
            throw e;
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<ApiResponse<Void>> revoke(@RequestBody TokenRefreshRequest request) {
        try {
            authService.revoke(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success(null, "Revoked successfully"));
        } catch (io.jsonwebtoken.JwtException e) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid token", Collections.emptyList()));
        }
    }
}
