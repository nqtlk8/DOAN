package com.store.erp.core.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserData user;

    @Data
    @Builder
    public static class UserData {
        private String username;
        private String role;
    }
}

