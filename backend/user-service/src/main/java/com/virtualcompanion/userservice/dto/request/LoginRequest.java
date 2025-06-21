package com.virtualcompanion.userservice.dto.request;

public class LoginRequest {
    
    @NotBlank(message = "Email is required")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private String twoFactorCode;
    
    private String deviceId;
    private String deviceType;
    private String deviceName;
    
    private Boolean rememberMe;
}
