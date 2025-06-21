package com.virtualcompanion.userservice.repository;

public interface UserService {
    User register(RegisterRequest request);
    UserResponse getUserProfile(UUID userId);
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);
    String uploadAvatar(UUID userId, MultipartFile file);
    void deleteAvatar(UUID userId);
    void changePassword(UUID userId, ChangePasswordRequest request);
    void verifyEmail(String token);
    void resendVerificationEmail(UUID userId);
    void verifyAge(UUID userId, AgeVerificationRequest request);
    List<UserPreferenceRequest> getUserPreferences(UUID userId);
    void updatePreferences(UUID userId, List<UserPreferenceRequest> preferences);
    String exportUserData(UUID userId, GdprExportRequest request);
    void deleteAccount(UUID userId, AccountDeletionRequest request);
    Page<UserResponse> searchUsers(String query, Pageable pageable);
}
