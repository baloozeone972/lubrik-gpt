package com.virtualcompanion.userservice.dto.request;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserResponse user;
    private Boolean requiresTwoFactor;
}
