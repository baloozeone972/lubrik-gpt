package com.virtualcompanion.userservice.service;

public interface AuthService {
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);

    void logout(String token);

    AuthResponse refreshToken(String refreshToken);

    boolean isEmailAvailable(String email);

    boolean isUsernameAvailable(String username);

    TokenValidationResponse validateToken(String token);
}
