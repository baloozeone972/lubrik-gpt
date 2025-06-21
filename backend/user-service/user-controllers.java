// UserController.java
package com.virtualcompanion.userservice.controller;

import com.virtualcompanion.userservice.dto.*;
import com.virtualcompanion.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User profile and settings management")
@SecurityRequirement(name = "bearer-jwt")
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

// AuthController.java
package com.virtualcompanion.userservice.controller;

import com.virtualcompanion.userservice.dto.*;
import com.virtualcompanion.userservice.service.AuthService;
import com.virtualcompanion.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and registration")
public class AuthController {
    
    private final AuthService authService;
    private final UserService userService;
    
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    @Operation(summary = "User logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String token) {
        // Remove "Bearer " prefix
        String jwtToken = token.substring(7);
        authService.logout(jwtToken);
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        userService.verifyEmail(request.getToken());
    }
    
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        userService.resendVerificationEmail(request.getEmail());
    }
    
    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.requestPasswordReset(request.getEmail());
    }
    
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
    }
    
    @GetMapping("/check-email")
    @Operation(summary = "Check if email is available")
    public ResponseEntity<EmailAvailabilityResponse> checkEmailAvailability(@RequestParam String email) {
        boolean available = authService.isEmailAvailable(email);
        return ResponseEntity.ok(new EmailAvailabilityResponse(email, available));
    }
    
    @GetMapping("/check-username")
    @Operation(summary = "Check if username is available")
    public ResponseEntity<UsernameAvailabilityResponse> checkUsernameAvailability(@RequestParam String username) {
        boolean available = authService.isUsernameAvailable(username);
        return ResponseEntity.ok(new UsernameAvailabilityResponse(username, available));
    }
    
    @PostMapping("/validate-token")
    @Operation(summary = "Validate JWT token")
    public ResponseEntity<TokenValidationResponse> validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        TokenValidationResponse response = authService.validateToken(request.getToken());
        return ResponseEntity.ok(response);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

// AdminController.java
package com.virtualcompanion.userservice.controller;

import com.virtualcompanion.userservice.dto.*;
import com.virtualcompanion.userservice.service.AdminService;
import com.virtualcompanion.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User Administration", description = "Admin operations for user management")
@SecurityRequirement(name = "bearer-jwt")
public class AdminController {
    
    private final UserService userService;
    private final AdminService adminService;
    
    @PostMapping("/{userId}/lock")
    @Operation(summary = "Lock user account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lockUserAccount(
            @PathVariable UUID userId,
            @Valid @RequestBody LockAccountRequest request) {
        userService.lockUserAccount(userId, request.getReason());
    }
    
    @PostMapping("/{userId}/unlock")
    @Operation(summary = "Unlock user account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlockUserAccount(@PathVariable UUID userId) {
        userService.unlockUserAccount(userId);
    }
    
    @PutMapping("/{userId}/subscription")
    @Operation(summary = "Update user subscription")
    public ResponseEntity<Void> updateUserSubscription(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        userService.updateSubscription(userId, request);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Get user statistics")
    public ResponseEntity<UserStatisticsSummary> getUserStatistics(
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        UserStatisticsSummary statistics = adminService.getUserStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/sessions")
    @Operation(summary = "Get active user sessions")
    public ResponseEntity<Page<UserSessionResponse>> getActiveSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<UserSessionResponse> sessions = adminService.getActiveSessions(pageRequest);
        return ResponseEntity.ok(sessions);
    }
    
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Invalidate user session")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invalidateSession(@PathVariable UUID sessionId) {
        adminService.invalidateSession(sessionId);
    }
    
    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<AuditLogResponse> logs = adminService.getAuditLogs(userId, action, startDate, endDate, pageRequest);
        return ResponseEntity.ok(logs);
    }
    
    @PostMapping("/bulk-operations")
    @Operation(summary = "Perform bulk operations on users")
    public ResponseEntity<BulkOperationResponse> performBulkOperation(
            @Valid @RequestBody BulkOperationRequest request) {
        BulkOperationResponse response = adminService.performBulkOperation(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/compliance-report")
    @Operation(summary = "Generate compliance report")
    public ResponseEntity<ComplianceReportResponse> generateComplianceReport(
            @RequestParam(required = false) String jurisdiction) {
        ComplianceReportResponse report = adminService.generateComplianceReport(jurisdiction);
        return ResponseEntity.ok(report);
    }
    
    @PostMapping("/export")
    @Operation(summary = "Export user data")
    public ResponseEntity<ExportResponse> exportUserData(
            @Valid @RequestBody ExportRequest request) {
        ExportResponse response = adminService.exportUserData(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/import")
    @Operation(summary = "Import user data")
    public ResponseEntity<ImportResponse> importUserData(
            @Valid @RequestBody ImportRequest request) {
        ImportResponse response = adminService.importUserData(request);
        return ResponseEntity.ok(response);
    }
}