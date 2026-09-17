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
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'HQ' or '${instance.role:ALL}' == 'ALL'")
@Slf4j
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody TokenRefreshRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/revoke")
    public ResponseEntity<ApiResponse<Void>> revoke(@RequestBody TokenRefreshRequest request) {
        authService.revoke(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Revoked successfully"));
    }
}
