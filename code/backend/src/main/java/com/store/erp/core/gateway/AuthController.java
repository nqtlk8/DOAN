package com.store.erp.core.gateway;

import com.store.erp.core.dto.ApiResponse;
import com.store.erp.core.dto.LoginRequest;
import com.store.erp.core.dto.LoginResponse;
import com.store.erp.core.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.store.erp.repos.UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        
        return userRepository.findByUsername(username).map(user -> {
            if (passwordEncoder.matches(password, user.getPasswordHash())) {
                String role = user.getRole();
                String accessToken = jwtUtil.generateToken(username, List.of(role));
                String refreshToken = UUID.randomUUID().toString(); // mock refresh token

                LoginResponse loginResponse = LoginResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .user(LoginResponse.UserData.builder()
                                .username(username)
                                .role(role)
                                .build())
                        .build();

                return ResponseEntity.ok(new ApiResponse<>(true, loginResponse, "Login successful", null));
            } else {
                return ResponseEntity.status(401).body(new ApiResponse<LoginResponse>(
                        false, null, "Invalid credentials", List.of("Wrong password")
                ));
            }
        }).orElseGet(() -> ResponseEntity.status(401).body(new ApiResponse<LoginResponse>(
                false, null, "Invalid credentials", List.of("Unknown username")
        )));
    }
}

