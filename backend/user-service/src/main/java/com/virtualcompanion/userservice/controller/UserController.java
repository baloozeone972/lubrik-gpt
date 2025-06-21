package com.virtualcompanion.userservice.controller;

public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping
    @Operation(summary = "Get all users (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UserResponse> users = userService.getAllUsers(pageRequest);
        return ResponseEntity.ok(users);
    }
    
    @PutMapping("/{userId}")
    @Operation(summary = "Update user profile")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse updatedUser = userService.updateUser(userId, request);
        return ResponseEntity.ok(updatedUser);
    }
    
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user account")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
    }
    
    @PostMapping("/{userId}/password")
    @Operation(summary = "Update user password")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<UserResponse> updatePassword(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        UserResponse user = userService.updatePassword(userId, request);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/{userId}/preferences")
    @Operation(summary = "Get user preferences")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserPreferenceResponse> getUserPreferences(@PathVariable UUID userId) {
        UserPreferenceResponse preferences = userService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }
    
    @PutMapping("/{userId}/preferences")
    @Operation(summary = "Update user preferences")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<UserPreferenceResponse> updateUserPreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdatePreferenceRequest request) {
        UserPreferenceResponse preferences = userService.updateUserPreferences(userId, request);
        return ResponseEntity.ok(preferences);
    }
    
    @GetMapping("/{userId}/compliance")
    @Operation(summary = "Get user compliance status")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<ComplianceStatusResponse> getComplianceStatus(@PathVariable UUID userId) {
        ComplianceStatusResponse compliance = userService.getComplianceStatus(userId);
        return ResponseEntity.ok(compliance);
    }
    
    @PutMapping("/{userId}/compliance")
    @Operation(summary = "Update user compliance consents")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<Void> updateComplianceStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateComplianceRequest request) {
        userService.updateComplianceStatus(userId, request);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{userId}/subscription")
    @Operation(summary = "Get user subscription details")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<SubscriptionResponse> getUserSubscription(@PathVariable UUID userId) {
        SubscriptionResponse subscription = userService.getUserSubscription(userId);
        return ResponseEntity.ok(subscription);
    }
    
    @GetMapping("/{userId}/statistics")
    @Operation(summary = "Get user statistics")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics(@PathVariable UUID userId) {
        UserStatisticsResponse statistics = userService.getUserStatistics(userId);
        return ResponseEntity.ok(statistics);
    }
    
    @PostMapping("/{userId}/2fa/enable")
    @Operation(summary = "Enable two-factor authentication")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<TwoFactorSetupResponse> enableTwoFactor(@PathVariable UUID userId) {
        userService.enableTwoFactorAuth(userId);
        String qrCode = userService.generateTwoFactorQrCode(userId);
        return ResponseEntity.ok(new TwoFactorSetupResponse(qrCode));
    }
    
    @PostMapping("/{userId}/2fa/disable")
    @Operation(summary = "Disable two-factor authentication")
    @PreAuthorize("#userId == authentication.principal.id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableTwoFactor(
            @PathVariable UUID userId,
            @Valid @RequestBody DisableTwoFactorRequest request) {
        userService.disableTwoFactorAuth(userId, request.getVerificationCode());
    }
    
    @PostMapping("/{userId}/2fa/verify")
    @Operation(summary = "Verify two-factor authentication code")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<VerifyTwoFactorResponse> verifyTwoFactor(
            @PathVariable UUID userId,
            @Valid @RequestBody VerifyTwoFactorRequest request) {
        boolean valid = userService.verifyTwoFactorCode(userId, request.getCode());
        return ResponseEntity.ok(new VerifyTwoFactorResponse(valid));
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search users (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<UserResponse> users = userService.searchUsers(query, pageRequest);
        return ResponseEntity.ok(users);
    }
}
