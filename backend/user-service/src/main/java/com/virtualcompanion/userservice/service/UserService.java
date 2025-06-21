package com.virtualcompanion.userservice.service;

public interface UserService {
    
    UserResponse createUser(RegisterRequest request);
    
    UserResponse getUserById(UUID userId);
    
    UserResponse getUserByEmail(String email);
    
    Page<UserResponse> getAllUsers(Pageable pageable);
    
    UserResponse updateUser(UUID userId, UpdateUserRequest request);
    
    void deleteUser(UUID userId);
    
    void verifyEmail(String verificationToken);
    
    void resendVerificationEmail(String email);
    
    UserResponse updatePassword(UUID userId, UpdatePasswordRequest request);
    
    void requestPasswordReset(String email);
    
    void resetPassword(String token, String newPassword);
    
    UserPreferenceResponse getUserPreferences(UUID userId);
    
    UserPreferenceResponse updateUserPreferences(UUID userId, UpdatePreferenceRequest request);
    
    ComplianceStatusResponse getComplianceStatus(UUID userId);
    
    void updateComplianceStatus(UUID userId, UpdateComplianceRequest request);
    
    SubscriptionResponse getUserSubscription(UUID userId);
    
    void updateSubscription(UUID userId, UpdateSubscriptionRequest request);
    
    Page<UserResponse> searchUsers(String query, Pageable pageable);
    
    void lockUserAccount(UUID userId, String reason);
    
    void unlockUserAccount(UUID userId);
    
    UserStatisticsResponse getUserStatistics(UUID userId);
    
    void enableTwoFactorAuth(UUID userId);
    
    void disableTwoFactorAuth(UUID userId, String verificationCode);
    
    String generateTwoFactorQrCode(UUID userId);
    
    boolean verifyTwoFactorCode(UUID userId, String code);
}
