package com.virtualcompanion.userservice.dto.response;

public class UserResponse {
    private UUID id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private UserStatus status;
    private SubscriptionLevel subscriptionLevel;
    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean ageVerified;
    private boolean twoFaEnabled;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
