// ===== VALIDATEURS PERSONNALISÉS =====

// ValidPassword.java
package com.virtualcompanion.userservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password does not meet security requirements";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// PasswordValidator.java
package com.virtualcompanion.userservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    @Value("${app.security.password-min-length:8}")
    private int minLength;
    
    @Value("${app.security.password-require-uppercase:true}")
    private boolean requireUppercase;
    
    @Value("${app.security.password-require-lowercase:true}")
    private boolean requireLowercase;
    
    @Value("${app.security.password-require-numbers:true}")
    private boolean requireNumbers;
    
    @Value("${app.security.password-require-special:true}")
    private boolean requireSpecial;
    
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile(".*[!@#$%^&*(),.?\":{}|<>].*");
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        
        context.disableDefaultConstraintViolation();
        
        if (password.length() < minLength) {
            context.buildConstraintViolationWithTemplate(
                String.format("Password must be at least %d characters long", minLength)
            ).addConstraintViolation();
            return false;
        }
        
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one uppercase letter"
            ).addConstraintViolation();
            return false;
        }
        
        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one lowercase letter"
            ).addConstraintViolation();
            return false;
        }
        
        if (requireNumbers && !NUMBER_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one number"
            ).addConstraintViolation();
            return false;
        }
        
        if (requireSpecial && !SPECIAL_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                "Password must contain at least one special character"
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}

// Adult.java
package com.virtualcompanion.userservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AdultValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Adult {
    String message() default "Must be at least {minimumAge} years old";
    int minimumAge() default 18;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// AdultValidator.java
package com.virtualcompanion.userservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;

public class AdultValidator implements ConstraintValidator<Adult, LocalDate> {
    
    private int minimumAge;
    
    @Override
    public void initialize(Adult constraintAnnotation) {
        this.minimumAge = constraintAnnotation.minimumAge();
    }
    
    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        if (birthDate == null) {
            return true; // Let @NotNull handle null validation
        }
        
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        return age >= minimumAge;
    }
}

// ===== CONTRÔLEURS REST =====

// AuthController.java
package com.virtualcompanion.userservice.controller;

import com.virtualcompanion.userservice.dto.request.*;
import com.virtualcompanion.userservice.dto.response.*;
import com.virtualcompanion.userservice.service.AuthService;
import com.virtualcompanion.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management endpoints")
public class AuthController {
    
    private final AuthService authService;
    private final UserService userService;
    
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }
    
    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    @PostMapping("/login/2fa")
    @Operation(summary = "Login with 2FA code")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWith2FA(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        AuthResponse response = authService.loginWith2FA(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }
    
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout user")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String token) {
        
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
    
    @GetMapping("/verify-email")
    @Operation(summary = "Verify email address")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token) {
        
        userService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }
    
    @PostMapping("/resend-verification")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Resend verification email")
    public ResponseEntity<ApiResponse<Void>> resendVerification() {
        
        userService.resendVerificationEmail();
        return ResponseEntity.ok(ApiResponse.success("Verification email sent", null));
    }
    
    @PostMapping("/password-reset")
    @Operation(summary = "Request password reset")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset email sent", null));
    }
    
    @PostMapping("/password-reset/confirm")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }
}

// UserController.java
package com.virtualcompanion.userservice.controller;

import com.virtualcompanion.userservice.dto.request.*;
import com.virtualcompanion.userservice.dto.response.*;
import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.service.UserService;
import com.virtualcompanion.userservice.service.SessionService;
import com.virtualcompanion.userservice.service.TwoFactorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User management endpoints")
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
            @AuthenticationPrincipal User currentUser) {
        
        List<SessionResponse> sessions = sessionService.getUserSessions(currentUser.getId());
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
        
        sessionService.revokeAllSessions(currentUser.getId(), token);
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

// AdminUserController.java
package com.virtualcompanion.userservice.controller;

import com.virtualcompanion.userservice.dto.response.*;
import com.virtualcompanion.userservice.service.UserService;
import com.virtualcompanion.userservice.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Users", description = "User administration endpoints")
public class AdminUserController {
    
    private final UserService userService;
    private final AdminService adminService;
    
    @GetMapping
    @Operation(summary = "Get all users (paginated)")
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        PageResponse<UserResponse> users = adminService.getUsers(pageable, search, status);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID userId) {
        UserResponse user = userService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend user")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable UUID userId,
            @RequestParam String reason) {
        
        adminService.suspendUser(userId, reason);
        return ResponseEntity.ok(ApiResponse.success("User suspended", null));
    }
    
    @PostMapping("/{userId}/activate")
    @Operation(summary = "Activate user")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable UUID userId) {
        adminService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User activated", null));
    }
    
    @PostMapping("/{userId}/verify-age")
    @Operation(summary = "Manually verify user age")
    public ResponseEntity<ApiResponse<Void>> verifyUserAge(
            @PathVariable UUID userId,
            @RequestParam String method) {
        
        adminService.verifyUserAge(userId, method);
        return ResponseEntity.ok(ApiResponse.success("Age verified", null));
    }
    
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete user (Super Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID userId,
            @RequestParam boolean immediate) {
        
        adminService.deleteUser(userId, immediate);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }
}