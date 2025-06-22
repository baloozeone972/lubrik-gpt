package com.virtualcompanion.userservice.controller;

public class UserController {
    
    private final UserService userService;
    private final SessionService sessionService;
    private final TwoFactorService twoFactorService;
    
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal User currentUser) {
        
        UserResponse response = userService.getUserProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/profile")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        UserResponse response = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }
    
    @PostMapping("/avatar")
    @Operation(summary = "Upload avatar image")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) {
        
        String avatarUrl = userService.uploadAvatar(currentUser.getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded", avatarUrl));
    }
    
    @DeleteMapping("/avatar")
    @Operation(summary = "Delete avatar image")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar(
            @AuthenticationPrincipal User currentUser) {
        
        userService.deleteAvatar(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Avatar deleted", null));
    }
    
    @PostMapping("/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        userService.changePassword(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
    
    @PostMapping("/verify-age")
    @Operation(summary = "Verify user age")
    public ResponseEntity<ApiResponse<Void>> verifyAge(
            @AuthenticationPrincipal User currentUser,
            @Valid @ModelAttribute AgeVerificationRequest request) {
        
        userService.verifyAge(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Age verification submitted", null));
    }
    
    // Sessions
    @GetMapping("/sessions")
    @Operation(summary = "Get user sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessions(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader("Authorization") String currentToken) {
        
        List<SessionResponse> sessions = sessionService.getUserSessions(currentUser.getId());
        
        // Mark current session
        String token = currentToken.replace("Bearer ", "");
        sessions.forEach(session -> {
            // In real implementation, would match by session token
            session.setCurrent(false);
        });
        
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }
    
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revoke a session")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID sessionId) {
        
        sessionService.revokeSession(currentUser.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session revoked", null));
    }
    
    @PostMapping("/sessions/revoke-all")
    @Operation(summary = "Revoke all sessions except current")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions(
            @AuthenticationPrincipal User currentUser,
            @RequestHeader("Authorization") String token) {
        
        sessionService.revokeAllSessions(currentUser.getId(), token.replace("Bearer ", ""));
        return ResponseEntity.ok(ApiResponse.success("All sessions revoked", null));
    }
    
    // Two-Factor Authentication
    @PostMapping("/2fa/enable")
    @Operation(summary = "Enable 2FA")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> enable2FA(
            @AuthenticationPrincipal User currentUser) {
        
        TwoFactorSetupResponse response = twoFactorService.setupTwoFactor(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/2fa/confirm")
    @Operation(summary = "Confirm 2FA setup")
    public ResponseEntity<ApiResponse<Void>> confirm2FA(
            @AuthenticationPrincipal User currentUser,
            @RequestParam String code) {
        
        twoFactorService.confirmTwoFactor(currentUser.getId(), code);
        return ResponseEntity.ok(ApiResponse.success("2FA enabled successfully", null));
    }
    
    @PostMapping("/2fa/disable")
    @Operation(summary = "Disable 2FA")
    public ResponseEntity<ApiResponse<Void>> disable2FA(
            @AuthenticationPrincipal User currentUser,
            @RequestParam String password) {
        
        twoFactorService.disableTwoFactor(currentUser.getId(), password);
        return ResponseEntity.ok(ApiResponse.success("2FA disabled", null));
    }
    
    // Preferences
    @GetMapping("/preferences")
    @Operation(summary = "Get user preferences")
    public ResponseEntity<ApiResponse<List<UserPreferenceRequest>>> getPreferences(
            @AuthenticationPrincipal User currentUser) {
        
        List<UserPreferenceRequest> preferences = userService.getUserPreferences(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }
    
    @PutMapping("/preferences")
    @Operation(summary = "Update user preferences")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody List<UserPreferenceRequest> preferences) {
        
        userService.updatePreferences(currentUser.getId(), preferences);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", null));
    }
    
    // GDPR
    @PostMapping("/gdpr/export")
    @Operation(summary = "Export user data (GDPR)")
    public ResponseEntity<ApiResponse<String>> exportData(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody GdprExportRequest request) {
        
        String exportUrl = userService.exportUserData(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Data export initiated", exportUrl));
    }
    
    @DeleteMapping("/gdpr/delete")
    @Operation(summary = "Delete account (GDPR)")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AccountDeletionRequest request) {
        
        userService.deleteAccount(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Account deletion initiated", null));
    }
}
