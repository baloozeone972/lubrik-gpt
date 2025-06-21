// ===== REPOSITORIES =====

// UserRepository.java
package com.virtualcompanion.userservice.repository;

import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
    
    @Query("SELECT u FROM User u WHERE u.email = :email OR u.username = :username")
    Optional<User> findByEmailOrUsername(@Param("email") String email, @Param("username") String username);
    
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.createdAt BETWEEN :startDate AND :endDate")
    Page<User> findByStatusAndCreatedAtBetween(
        @Param("status") UserStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status AND u.createdAt BETWEEN :startDate AND :endDate")
    long countByStatusAndCreatedAtBetween(
        @Param("status") UserStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt, u.lastLoginIp = :lastLoginIp WHERE u.id = :userId")
    void updateLastLogin(
        @Param("userId") UUID userId,
        @Param("lastLoginAt") LocalDateTime lastLoginAt,
        @Param("lastLoginIp") String lastLoginIp
    );
    
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.emailVerified = false AND u.createdAt < :cutoffDate")
    List<User> findUnverifiedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL AND u.lockedUntil < :now")
    List<User> findLockedUsersToUnlock(@Param("now") LocalDateTime now);
}

// UserSessionRepository.java
package com.virtualcompanion.userservice.repository;

import com.virtualcompanion.userservice.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    
    Optional<UserSession> findBySessionToken(String sessionToken);
    
    List<UserSession> findByUserIdAndRevokedFalseOrderByCreatedAtDesc(UUID userId);
    
    @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId AND s.revoked = false AND s.expiresAt > :now")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.user.id = :userId AND s.sessionToken != :currentToken")
    void revokeAllUserSessionsExcept(@Param("userId") UUID userId, @Param("currentToken") String currentToken);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivityAt = :now WHERE s.sessionToken = :sessionToken")
    void updateLastActivity(@Param("sessionToken") String sessionToken, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now OR s.revoked = true")
    void deleteExpiredAndRevokedSessions(@Param("now") LocalDateTime now);
}

// VerificationTokenRepository.java
package com.virtualcompanion.userservice.repository;

import com.virtualcompanion.userservice.entity.TokenType;
import com.virtualcompanion.userservice.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    
    Optional<VerificationToken> findByToken(String token);
    
    Optional<VerificationToken> findByTokenAndTokenType(String token, TokenType tokenType);
    
    @Query("SELECT t FROM VerificationToken t WHERE t.user.id = :userId AND t.tokenType = :tokenType AND t.usedAt IS NULL AND t.expiresAt > :now")
    Optional<VerificationToken> findValidTokenByUserAndType(
        @Param("userId") UUID userId,
        @Param("tokenType") TokenType tokenType,
        @Param("now") LocalDateTime now
    );
    
    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.expiresAt < :now OR t.usedAt IS NOT NULL")
    void deleteExpiredAndUsedTokens(@Param("now") LocalDateTime now);
}

// UserPreferenceRepository.java
package com.virtualcompanion.userservice.repository;

import com.virtualcompanion.userservice.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {
    
    List<UserPreference> findByUserId(UUID userId);
    
    Optional<UserPreference> findByUserIdAndKey(UUID userId, String key);
    
    void deleteByUserIdAndKey(UUID userId, String key);
}

// ===== SERVICES =====

// UserService.java
package com.virtualcompanion.userservice.service;

import com.virtualcompanion.userservice.dto.request.*;
import com.virtualcompanion.userservice.dto.response.*;
import com.virtualcompanion.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

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

// UserServiceImpl.java
package com.virtualcompanion.userservice.service.impl;

import com.virtualcompanion.userservice.dto.request.*;
import com.virtualcompanion.userservice.dto.response.*;
import com.virtualcompanion.userservice.entity.*;
import com.virtualcompanion.userservice.exception.*;
import com.virtualcompanion.userservice.mapper.UserMapper;
import com.virtualcompanion.userservice.repository.*;
import com.virtualcompanion.userservice.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${app.age-verification.enabled}")
    private boolean ageVerificationEnabled;
    
    @Value("${app.age-verification.minimum-age}")
    private int minimumAge;
    
    @Override
    public User register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        
        // Validate uniqueness
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already taken");
        }
        
        // Create user
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .birthDate(request.getBirthDate())
                .phoneNumber(request.getPhoneNumber())
                .status(UserStatus.PENDING_VERIFICATION)
                .subscriptionLevel(SubscriptionLevel.FREE)
                .roles(Set.of(UserRole.USER))
                .build();
        
        // Create compliance record
        UserCompliance compliance = UserCompliance.builder()
                .user(user)
                .termsAcceptedVersion("1.0")
                .termsAcceptedAt(LocalDateTime.now())
                .privacyAcceptedVersion("1.0")
                .privacyAcceptedAt(LocalDateTime.now())
                .marketingConsent(request.getMarketingConsent() != null && request.getMarketingConsent())
                .dataProcessingConsent(true)
                .build();
        
        user.setCompliance(compliance);
        user = userRepository.save(user);
        
        // Send verification email
        createAndSendVerificationToken(user, TokenType.EMAIL_VERIFICATION);
        
        // Publish event
        publishUserEvent("USER_REGISTERED", user.getId(), null);
        
        log.info("User registered successfully with ID: {}", user.getId());
        return user;
    }
    
    @Override
    public UserResponse getUserProfile(UUID userId) {
        User user = getUserById(userId);
        return userMapper.toResponse(user);
    }
    
    @Override
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUserById(userId);
        
        // Update fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        user = userRepository.save(user);
        
        publishUserEvent("USER_PROFILE_UPDATED", user.getId(), null);
        
        return userMapper.toResponse(user);
    }
    
    @Override
    public String uploadAvatar(UUID userId, MultipartFile file) {
        User user = getUserById(userId);
        
        // Validate file
        if (!isValidImageFile(file)) {
            throw new ValidationException("Invalid image file");
        }
        
        // Delete old avatar if exists
        if (user.getAvatarUrl() != null) {
            fileStorageService.deleteFile(user.getAvatarUrl());
        }
        
        // Upload new avatar
        String avatarUrl = fileStorageService.uploadFile(file, "avatars/" + userId);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        
        return avatarUrl;
    }
    
    @Override
    public void deleteAvatar(UUID userId) {
        User user = getUserById(userId);
        
        if (user.getAvatarUrl() != null) {
            fileStorageService.deleteFile(user.getAvatarUrl());
            user.setAvatarUrl(null);
            userRepository.save(user);
        }
    }
    
    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getUserById(userId);
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        
        // Verify new password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("New password and confirmation do not match");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Send notification
        emailService.sendPasswordChangedEmail(user);
        
        publishUserEvent("USER_PASSWORD_CHANGED", user.getId(), null);
    }
    
    @Override
    public void verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));
        
        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("Verification token has expired");
        }
        
        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("Verification token has already been used");
        }
        
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        
        verificationToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(verificationToken);
        
        emailService.sendWelcomeEmail(user);
        publishUserEvent("USER_EMAIL_VERIFIED", user.getId(), null);
    }
    
    @Override
    public void resendVerificationEmail(UUID userId) {
        User user = getUserById(userId);
        
        if (user.isEmailVerified()) {
            throw new ValidationException("Email is already verified");
        }
        
        // Check if there's a recent token
        Optional<VerificationToken> existingToken = tokenRepository.findValidTokenByUserAndType(
                userId, TokenType.EMAIL_VERIFICATION, LocalDateTime.now()
        );
        
        if (existingToken.isPresent()) {
            LocalDateTime createdAt = existingToken.get().getCreatedAt();
            if (createdAt.isAfter(LocalDateTime.now().minusMinutes(5))) {
                throw new ValidationException("Please wait before requesting another verification email");
            }
        }
        
        createAndSendVerificationToken(user, TokenType.EMAIL_VERIFICATION);
    }
    
    @Override
    public void verifyAge(UUID userId, AgeVerificationRequest request) {
        User user = getUserById(userId);
        
        if (user.isAgeVerified()) {
            throw new ValidationException("Age is already verified");
        }
        
        // Process verification based on method
        AgeVerificationResult result = switch (request.getMethod()) {
            case DOCUMENT -> processDocumentVerification(user, request);
            case CREDIT_CARD -> processCreditCardVerification(user, request);
            case BANK_ACCOUNT -> processBankAccountVerification(user, request);
            case BIOMETRIC -> processBiometricVerification(user, request);
        };
        
        if (result.isVerified()) {
            user.setAgeVerified(true);
            userRepository.save(user);
            
            publishUserEvent("USER_AGE_VERIFIED", user.getId(), 
                    Map.of("method", request.getMethod().toString()));
        }
    }
    
    @Override
    public List<UserPreferenceRequest> getUserPreferences(UUID userId) {
        List<UserPreference> preferences = preferenceRepository.findByUserId(userId);
        
        return preferences.stream()
                .map(pref -> UserPreferenceRequest.builder()
                        .key(pref.getKey())
                        .value(pref.getValue())
                        .type(pref.getType())
                        .build())
                .collect(Collectors.toList());
    }
    
    @Override
    public void updatePreferences(UUID userId, List<UserPreferenceRequest> preferences) {
        User user = getUserById(userId);
        
        for (UserPreferenceRequest prefRequest : preferences) {
            UserPreference preference = preferenceRepository
                    .findByUserIdAndKey(userId, prefRequest.getKey())
                    .orElse(UserPreference.builder()
                            .user(user)
                            .key(prefRequest.getKey())
                            .build());
            
            preference.setValue(prefRequest.getValue());
            preference.setType(prefRequest.getType());
            preferenceRepository.save(preference);
        }
    }
    
    @Override
    public String exportUserData(UUID userId, GdprExportRequest request) {
        User user = getUserById(userId);
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Invalid password");
        }
        
        // Update GDPR request count
        UserCompliance compliance = user.getCompliance();
        compliance.setGdprDataRequestCount(compliance.getGdprDataRequestCount() + 1);
        compliance.setLastGdprRequestAt(LocalDateTime.now());
        
        // Generate export
        String exportUrl = generateUserDataExport(user, request.getFormat());
        
        // Send email with download link
        emailService.sendDataExportEmail(user, exportUrl);
        
        publishUserEvent("USER_DATA_EXPORTED", user.getId(), 
                Map.of("format", request.getFormat().toString()));
        
        return exportUrl;
    }
    
    @Override
    public void deleteAccount(UUID userId, AccountDeletionRequest request) {
        User user = getUserById(userId);
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Invalid password");
        }
        
        // Process deletion
        if (request.getImmediateDelete()) {
            // Immediate anonymization
            anonymizeUser(user);
        } else {
            // Schedule deletion after grace period (30 days)
            user.setStatus(UserStatus.DELETED);
            user.setDeletedAt(LocalDateTime.now());
        }
        
        userRepository.save(user);
        
        // Cancel subscriptions
        kafkaTemplate.send("billing-events", Map.of(
                "eventType", "CANCEL_SUBSCRIPTION",
                "userId", userId
        ));
        
        // Send confirmation email
        emailService.sendAccountDeletionEmail(user);
        
        publishUserEvent("USER_ACCOUNT_DELETED", user.getId(), 
                Map.of("reason", request.getReason()));
    }
    
    @Override
    public Page<UserResponse> searchUsers(String query, Pageable pageable) {
        // This would typically integrate with Elasticsearch
        // For now, simple database search
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toResponse);
    }
    
    // Helper methods
    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    private void createAndSendVerificationToken(User user, TokenType tokenType) {
        String token = UUID.randomUUID().toString();
        
        VerificationToken verificationToken = VerificationToken.builder()
                .user(user)
                .token(token)
                .tokenType(tokenType)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        
        tokenRepository.save(verificationToken);
        
        switch (tokenType) {
            case EMAIL_VERIFICATION -> emailService.sendVerificationEmail(user, token);
            case PASSWORD_RESET -> emailService.sendPasswordResetEmail(user, token);
            case PHONE_VERIFICATION -> smsService.sendVerificationSms(user, token);
        }
    }
    
    private void publishUserEvent(String eventType, UUID userId, Map<String, Object> metadata) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("userId", userId);
        event.put("timestamp", LocalDateTime.now());
        if (metadata != null) {
            event.put("metadata", metadata);
        }
        
        kafkaTemplate.send("user-events", event);
    }
    
    private boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/webp")
        );
    }
    
    private void anonymizeUser(User user) {
        String anonymousId = "DELETED_" + user.getId();
        user.setEmail(anonymousId + "@deleted.com");
        user.setUsername(anonymousId);
        user.setFirstName("DELETED");
        user.setLastName("USER");
        user.setPhoneNumber(null);
        user.setAvatarUrl(null);
        user.setPassword("DELETED");
        user.setTwoFaSecret(null);
        user.setStatus(UserStatus.DELETED);
    }
    
    private String generateUserDataExport(User user, GdprExportRequest.ExportFormat format) {
        // Implementation would generate JSON/CSV/PDF
        // For now, return mock URL
        return "https://exports.virtualcompanion.app/users/" + user.getId() + "/" + UUID.randomUUID() + "." + format.toString().toLowerCase();
    }
    
    private AgeVerificationResult processDocumentVerification(User user, AgeVerificationRequest request) {
        // Implementation would process document with OCR/AI
        return new AgeVerificationResult(true, 21, "DOCUMENT");
    }
    
    private AgeVerificationResult processCreditCardVerification(User user, AgeVerificationRequest request) {
        // Implementation would verify with payment processor
        return new AgeVerificationResult(true, 18, "CREDIT_CARD");
    }
    
    private AgeVerificationResult processBankAccountVerification(User user, AgeVerificationRequest request) {
        // Implementation would verify with banking API
        return new AgeVerificationResult(true, 18, "BANK_ACCOUNT");
    }
    
    private AgeVerificationResult processBiometricVerification(User user, AgeVerificationRequest request) {
        // Implementation would use facial age estimation
        return new AgeVerificationResult(true, 22, "BIOMETRIC");
    }
    
    @Value
    private static class AgeVerificationResult {
        boolean verified;
        int estimatedAge;
        String method;
    }
}

// EmailService.java
package com.virtualcompanion.userservice.service;

import com.virtualcompanion.userservice.entity.User;

public interface EmailService {
    void sendVerificationEmail(User user, String token);
    void sendPasswordResetEmail(User user, String token);
    void sendWelcomeEmail(User user);
    void sendPasswordChangedEmail(User user);
    void sendAccountLockedEmail(User user);
    void sendDataExportEmail(User user, String downloadUrl);
    void sendAccountDeletionEmail(User user);
}

// EmailServiceImpl.java
package com.virtualcompanion.userservice.service.impl;

import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.email.from-name}")
    private String fromName;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    @Override
    public void sendVerificationEmail(User user, String token) {
        String subject = "Vérifiez votre adresse email";
        String verificationUrl = baseUrl + "/verify-email?token=" + token;
        
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("verificationUrl", verificationUrl);
        
        sendHtmlEmail(user.getEmail(), subject, "email/verification", context);
    }
    
    @Override
    public void sendPasswordResetEmail(User user, String token) {
        String subject = "Réinitialisation de votre mot de passe";
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("resetUrl", resetUrl);
        
        sendHtmlEmail(user.getEmail(), subject, "email/password-reset", context);
    }
    
    @Override
    public void sendWelcomeEmail(User user) {
        String subject = "Bienvenue sur Virtual Companion !";
        
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("dashboardUrl", baseUrl + "/dashboard");
        
        sendHtmlEmail(user.getEmail(), subject, "email/welcome", context);
    }
    
    @Override
    public void sendPasswordChangedEmail(User user) {
        String subject = "Votre mot de passe a été modifié";
        
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("supportUrl", baseUrl + "/support");
        
        sendHtmlEmail(user.getEmail(), subject, "email/password-changed", context);
    }
    
    @Override
    public void sendAccountLockedEmail(User user) {
        String subject = "Votre compte a été temporairement verrouillé";
        
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("unlockTime", user.getLockedUntil());
        
        sendHtmlEmail(user.getEmail(), subject, "email/account-locked", context);
    }
    
    @Override
    public void sendDataExportEmail(User user, String downloadUrl) {
        String subject = "Vos données sont prêtes à être téléchargées";
        
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("downloadUrl", downloadUrl);
        
        sendHtmlEmail(user.getEmail(), subject, "email/data-export", context);
    }
    
    @Override
    public void sendAccountDeletionEmail(User user) {
        String subject = "Confirmation de suppression de compte";
        
        Context context = new Context();
        context.setVariable("user", user);
        context.setVariable("recoveryUrl", baseUrl + "/recover-account");
        
        sendHtmlEmail(user.getEmail(), subject, "email/account-deletion", context);
    }
    
    private void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            
            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
            
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new EmailSendException("Failed to send email", e);
        }
    }
}

// TwoFactorService.java
package com.virtualcompanion.userservice.service;

import com.virtualcompanion.userservice.dto.response.TwoFactorSetupResponse;
import com.virtualcompanion.userservice.entity.User;

import java.util.UUID;

public interface TwoFactorService {
    TwoFactorSetupResponse setupTwoFactor(UUID userId);
    void confirmTwoFactor(UUID userId, String code);
    void disableTwoFactor(UUID userId, String password);
    boolean verifyCode(User user, String code);
    String generateBackupCodes(User user);
}

// TwoFactorServiceImpl.java
package com.virtualcompanion.userservice.service.impl;

import com.virtualcompanion.userservice.dto.response.TwoFactorSetupResponse;
import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.exception.*;
import com.virtualcompanion.userservice.repository.UserRepository;
import com.virtualcompanion.userservice.service.TwoFactorService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TwoFactorServiceImpl implements TwoFactorService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
    
    @Value("${app.twofa.issuer}")
    private String issuer;
    
    @Override
    public TwoFactorSetupResponse setupTwoFactor(UUID userId) {
        User user = getUserById(userId);
        
        if (user.isTwoFaEnabled()) {
            throw new ValidationException("Two-factor authentication is already enabled");
        }
        
        // Generate secret
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();
        
        // Generate QR code URL
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                issuer,
                user.getEmail(),
                key
        );
        
        // Generate backup codes
        List<String> backupCodes = generateBackupCodesList();
        
        // Store secret temporarily (will be confirmed later)
        user.setTwoFaSecret(secret);
        userRepository.save(user);
        
        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .backupCodes(backupCodes)
                .build();
    }
    
    @Override
    public void confirmTwoFactor(UUID userId, String code) {
        User user = getUserById(userId);
        
        if (user.getTwoFaSecret() == null) {
            throw new ValidationException("Two-factor setup not initiated");
        }
        
        // Verify code
        if (!googleAuthenticator.authorize(user.getTwoFaSecret(), Integer.parseInt(code))) {
            throw new Invalid2FACodeException("Invalid verification code");
        }
        
        // Enable 2FA
        user.setTwoFaEnabled(true);
        userRepository.save(user);
        
        log.info("Two-factor authentication enabled for user: {}", userId);
    }
    
    @Override
    public void disableTwoFactor(UUID userId, String password) {
        User user = getUserById(userId);
        
        if (!user.isTwoFaEnabled()) {
            throw new ValidationException("Two-factor authentication is not enabled");
        }
        
        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidPasswordException("Invalid password");
        }
        
        // Disable 2FA
        user.setTwoFaEnabled(false);
        user.setTwoFaSecret(null);
        userRepository.save(user);
        
        log.info("Two-factor authentication disabled for user: {}", userId);
    }
    
    @Override
    public boolean verifyCode(User user, String code) {
        if (!user.isTwoFaEnabled() || user.getTwoFaSecret() == null) {
            return false;
        }
        
        try {
            int verificationCode = Integer.parseInt(code);
            return googleAuthenticator.authorize(user.getTwoFaSecret(), verificationCode);
        } catch (NumberFormatException e) {
            // Check if it's a backup code
            return verifyBackupCode(user, code);
        }
    }
    
    @Override
    public String generateBackupCodes(User user) {
        List<String> codes = generateBackupCodesList();
        // In a real implementation, these would be stored encrypted
        return String.join(",", codes);
    }
    
    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    private List<String> generateBackupCodesList() {
        SecureRandom random = new SecureRandom();
        return IntStream.range(0, 10)
                .mapToObj(i -> generateBackupCode(random))
                .collect(Collectors.toList());
    }
    
    private String generateBackupCode(SecureRandom random) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < 8; i++) {
            if (i == 4) code.append("-");
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return code.toString();
    }
    
    private boolean verifyBackupCode(User user, String code) {
        // In a real implementation, backup codes would be stored and checked
        return false;
    }
}

// SessionService.java
package com.virtualcompanion.userservice.service;

import com.virtualcompanion.userservice.dto.request.LoginRequest;
import com.virtualcompanion.userservice.dto.response.SessionResponse;
import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.entity.UserSession;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;

public interface SessionService {
    UserSession createSession(User user, LoginRequest loginRequest, HttpServletRequest request);
    List<SessionResponse> getUserSessions(UUID userId);
    void revokeSession(UUID userId, UUID sessionId);
    void revokeAllSessions(UUID userId, String exceptToken);
    void updateSessionActivity(String sessionToken);
    void cleanupExpiredSessions();
}

// SessionServiceImpl.java
package com.virtualcompanion.userservice.service.impl;

import com.virtualcompanion.userservice.dto.request.LoginRequest;
import com.virtualcompanion.userservice.dto.response.SessionResponse;
import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.entity.UserSession;
import com.virtualcompanion.userservice.exception.ResourceNotFoundException;
import com.virtualcompanion.userservice.repository.UserSessionRepository;
import com.virtualcompanion.userservice.service.SessionService;
import com.virtualcompanion.userservice.util.IpUtils;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SessionServiceImpl implements SessionService {
    
    private final UserSessionRepository sessionRepository;
    private final GeoLocationService geoLocationService;
    
    @Value("${app.jwt.expiration-hours}")
    private int sessionExpirationHours;
    
    @Override
    public UserSession createSession(User user, LoginRequest loginRequest, HttpServletRequest request) {
        String ipAddress = IpUtils.getClientIp(request);
        String userAgentString = request.getHeader("User-Agent");
        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        
        // Get location from IP
        GeoLocation location = geoLocationService.getLocationFromIp(ipAddress);
        
        UserSession session = UserSession.builder()
                .user(user)
                .sessionToken(UUID.randomUUID().toString())
                .refreshToken(UUID.randomUUID().toString())
                .deviceId(loginRequest.getDeviceId())
                .deviceType(userAgent.getOperatingSystem().getDeviceType().getName())
                .deviceName(loginRequest.getDeviceName() != null ? loginRequest.getDeviceName() : userAgent.getBrowser().getName())
                .ipAddress(ipAddress)
                .userAgent(userAgentString)
                .locationCountry(location.getCountryCode())
                .locationCity(location.getCity())
                .expiresAt(LocalDateTime.now().plusHours(sessionExpirationHours))
                .build();
        
        return sessionRepository.save(session);
    }
    
    @Override
    public List<SessionResponse> getUserSessions(UUID userId) {
        List<UserSession> sessions = sessionRepository.findActiveSessionsByUserId(userId, LocalDateTime.now());
        
        return sessions.stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void revokeSession(UUID userId, UUID sessionId) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        
        if (!session.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Cannot revoke session of another user");
        }
        
        session.setRevoked(true);
        sessionRepository.save(session);
    }
    
    @Override
    public void revokeAllSessions(UUID userId, String exceptToken) {
        if (exceptToken != null) {
            sessionRepository.revokeAllUserSessionsExcept(userId, exceptToken);
        } else {
            List<UserSession> sessions = sessionRepository.findByUserIdAndRevokedFalseOrderByCreatedAtDesc(userId);
            sessions.forEach(session -> {
                session.setRevoked(true);
                sessionRepository.save(session);
            });
        }
    }
    
    @Override
    public void updateSessionActivity(String sessionToken) {
        sessionRepository.updateLastActivity(sessionToken, LocalDateTime.now());
    }
    
    @Override
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void cleanupExpiredSessions() {
        log.info("Starting expired sessions cleanup");
        sessionRepository.deleteExpiredAndRevokedSessions(LocalDateTime.now());
        log.info("Expired sessions cleanup completed");
    }
    
    private SessionResponse mapToSessionResponse(UserSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .deviceId(session.getDeviceId())
                .deviceType(session.getDeviceType())
                .deviceName(session.getDeviceName())
                .ipAddress(session.getIpAddress())
                .locationCountry(session.getLocationCountry())
                .locationCity(session.getLocationCity())
                .createdAt(session.getCreatedAt())
                .lastActivityAt(session.getLastActivityAt())
                .expiresAt(session.getExpiresAt())
                .current(false) // Would be set by controller based on current token
                .build();
    }
}

// ===== CONTROLLERS =====

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
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @AuthenticationPrincipal User currentUser) {
        
        userService.resendVerificationEmail(currentUser.getId());
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

// UserController.java (complété)
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

// AdminUserController.java (complété)
package com.virtualcompanion.userservice.controller;

import com.virtualcompanion.userservice.dto.response.*;
import com.virtualcompanion.userservice.service.UserService;
import com.virtualcompanion.userservice.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
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
    
    @GetMapping("/statistics")
    @Operation(summary = "Get user statistics")
    public ResponseEntity<ApiResponse<UserStatistics>> getUserStatistics() {
        UserStatistics stats = adminService.getUserStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

// ===== MAPPER =====

// UserMapper.java
package com.virtualcompanion.userservice.mapper;

import com.virtualcompanion.userservice.dto.response.UserResponse;
import com.virtualcompanion.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    @Mapping(target = "roles", expression = "java(user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()))")
    UserResponse toResponse(User user);
}

// ===== EXCEPTIONS =====

// UserNotFoundException.java
package com.virtualcompanion.userservice.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

// EmailAlreadyExistsException.java
package com.virtualcompanion.userservice.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}

// UsernameAlreadyExistsException.java
package com.virtualcompanion.userservice.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}

// InvalidPasswordException.java
package com.virtualcompanion.userservice.exception;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}

// InvalidTokenException.java
package com.virtualcompanion.userservice.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}

// Invalid2FACodeException.java
package com.virtualcompanion.userservice.exception;

public class Invalid2FACodeException extends RuntimeException {
    public Invalid2FACodeException(String message) {
        super(message);
    }
}

// ValidationException.java
package com.virtualcompanion.userservice.exception;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}

// AccessDeniedException.java
package com.virtualcompanion.userservice.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}

// ResourceNotFoundException.java
package com.virtualcompanion.userservice.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// EmailSendException.java
package com.virtualcompanion.userservice.exception;

public class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}

// AccountLockedException.java
package com.virtualcompanion.userservice.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}

// ===== UTILS =====

// IpUtils.java
package com.virtualcompanion.userservice.util;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtils {
    
    private static final String[] IP_HEADERS = {
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    };
    
    public static String getClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle multiple IPs in X-Forwarded-For
                int commaIndex = ip.indexOf(',');
                if (commaIndex > 0) {
                    ip = ip.substring(0, commaIndex);
                }
                return ip.trim();
            }
        }
        return request.getRemoteAddr();
    }
}

// ===== TESTS UNITAIRES =====

// UserServiceImplTest.java
package com.virtualcompanion.userservice.service.impl;

import com.virtualcompanion.userservice.dto.request.*;
import com.virtualcompanion.userservice.entity.*;
import com.virtualcompanion.userservice.exception.*;
import com.virtualcompanion.userservice.mapper.UserMapper;
import com.virtualcompanion.userservice.repository.*;
import com.virtualcompanion.userservice.service.EmailService;
import com.virtualcompanion.userservice.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserPreferenceRepository preferenceRepository;
    
    @Mock
    private VerificationTokenRepository tokenRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private FileStorageService fileStorageService;
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private RegisterRequest validRegisterRequest;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "ageVerificationEnabled", true);
        ReflectionTestUtils.setField(userService, "minimumAge", 18);
        
        validRegisterRequest = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("SecurePass123!")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .acceptedTerms(true)
                .build();
        
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .password("encoded_password")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }
    
    @Test
    void register_Success() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        
        // When
        User result = userService.register(validRegisterRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(result.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
        
        verify(userRepository).save(any(User.class));
        verify(tokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendVerificationEmail(any(User.class), anyString());
        verify(kafkaTemplate).send(eq("user-events"), any(Map.class));
    }
    
    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> userService.register(validRegisterRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already registered");
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void register_UsernameAlreadyExists_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> userService.register(validRegisterRequest))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessage("Username already taken");
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void changePassword_Success() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword")
                .newPassword("NewSecurePass123!")
                .confirmPassword("NewSecurePass123!")
                .build();
        
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewSecurePass123!")).thenReturn("new_encoded_password");
        
        // When
        userService.changePassword(testUser.getId(), request);
        
        // Then
        verify(userRepository).save(argThat(user -> 
            user.getPassword().equals("new_encoded_password")
        ));
        verify(emailService).sendPasswordChangedEmail(testUser);
    }
    
    @Test
    void changePassword_WrongCurrentPassword_ThrowsException() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongPassword")
                .newPassword("NewSecurePass123!")
                .confirmPassword("NewSecurePass123!")
                .build();
        
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> userService.changePassword(testUser.getId(), request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Current password is incorrect");
    }
    
    @Test
    void verifyEmail_Success() {
        // Given
        String token = "valid-token";
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(testUser)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        testUser.setEmailVerified(false);
        testUser.setStatus(UserStatus.PENDING_VERIFICATION);
        
        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));
        
        // When
        userService.verifyEmail(token);
        
        // Then
        assertThat(testUser.isEmailVerified()).isTrue();
        assertThat(testUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(testUser);
        verify(emailService).sendWelcomeEmail(testUser);
    }
    
    @Test
    void verifyEmail_ExpiredToken_ThrowsException() {
        // Given
        String token = "expired-token";
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(testUser)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        
        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));
        
        // When/Then
        assertThatThrownBy(() -> userService.verifyEmail(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Verification token has expired");
    }
    
    @Test
    void uploadAvatar_Success() throws Exception {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/jpeg");
        
        String avatarUrl = "https://cdn.example.com/avatars/user-id.jpg";
        
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(fileStorageService.uploadFile(file, "avatars/" + testUser.getId())).thenReturn(avatarUrl);
        
        // When
        String result = userService.uploadAvatar(testUser.getId(), file);
        
        // Then
        assertThat(result).isEqualTo(avatarUrl);
        assertThat(testUser.getAvatarUrl()).isEqualTo(avatarUrl);
        verify(userRepository).save(testUser);
    }
    
    @Test
    void deleteAccount_ImmediateDelete_Success() {
        // Given
        AccountDeletionRequest request = AccountDeletionRequest.builder()
                .password("correctPassword")
                .reason("No longer needed")
                .immediateDelete(true)
                .build();
        
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getPassword(), testUser.getPassword())).thenReturn(true);
        
        // When
        userService.deleteAccount(testUser.getId(), request);
        
        // Then
        assertThat(testUser.getEmail()).startsWith("DELETED_");
        assertThat(testUser.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(kafkaTemplate).send(eq("billing-events"), any(Map.class));
        verify(emailService).sendAccountDeletionEmail(testUser);
    }
}

// AuthServiceImplTest.java
package com.virtualcompanion.userservice.service.impl;

import com.virtualcompanion.userservice.dto.request.*;
import com.virtualcompanion.userservice.dto.response.AuthResponse;
import com.virtualcompanion.userservice.entity.*;
import com.virtualcompanion.userservice.exception.BadCredentialsException;
import com.virtualcompanion.userservice.mapper.UserMapper;
import com.virtualcompanion.userservice.repository.*;
import com.virtualcompanion.userservice.security.JwtTokenProvider;
import com.virtualcompanion.userservice.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserSessionRepository sessionRepository;
    
    @Mock
    private VerificationTokenRepository tokenRepository;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private UserService userService;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private SessionService sessionService;
    
    @Mock
    private TwoFactorService twoFactorService;
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Mock
    private HttpServletRequest httpServletRequest;
    
    @InjectMocks
    private AuthServiceImpl authService;
    
    private User testUser;
    private LoginRequest loginRequest;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .password("encoded_password")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .twoFaEnabled(false)
                .build();
        
        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();
    }
    
    @Test
    void login_Success() {
        // Given
        Authentication authentication = mock(Authentication.class);
        UserSession session = UserSession.builder()
                .sessionToken("session-token")
                .build();
        
        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(sessionService.createSession(eq(testUser), eq(loginRequest), eq(httpServletRequest)))
                .thenReturn(session);
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(testUser, session.getSessionToken()))
                .thenReturn("refresh-token");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(3600000L);
        
        // When
        AuthResponse response = authService.login(loginRequest, httpServletRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRequiresTwoFactor()).isFalse();
        
        verify(userRepository).save(argThat(user -> 
            user.getFailedLoginAttempts() == 0 && user.getLockedUntil() == null
        ));
    }
    
    @Test
    void login_With2FA_ReturnsRequires2FA() {
        // Given
        testUser.setTwoFaEnabled(true);
        Authentication authentication = mock(Authentication.class);
        
        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        
        // When
        AuthResponse response = authService.login(loginRequest, httpServletRequest);
        
        // Then
        assertThat(response.getRequiresTwoFactor()).isTrue();
        assertThat(response.getAccessToken()).isNull();
    }
    
    @Test
    void login_AccountLocked_ThrowsException() {
        // Given
        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        
        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase()))
                .thenReturn(Optional.of(testUser));
        
        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest, httpServletRequest))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Account is locked until");
    }
    
    @Test
    void login_InvalidCredentials_IncrementsFailedAttempts() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        
        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest, httpServletRequest))
                .isInstanceOf(BadCredentialsException.class);
        
        verify(userRepository).save(argThat(user -> 
            user.getFailedLoginAttempts() == 1
        ));
    }
    
    @Test
    void refreshToken_Success() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        String sessionToken = "session-token";
        UserSession session = UserSession.builder()
                .sessionToken(sessionToken)
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .revoked(false)
                .build();
        
        when(jwtTokenProvider.validateToken(request.getRefreshToken())).thenReturn(true);
        when(jwtTokenProvider.getSessionTokenFromJwt(request.getRefreshToken())).thenReturn(sessionToken);
        when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(3600000L);
        
        // When
        AuthResponse response = authService.refreshToken(request);
        
        // Then
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(request.getRefreshToken());
        verify(sessionRepository).save(session);
    }
}

// ===== TESTS D'INTÉGRATION =====

// UserControllerIntegrationTest.java
package com.virtualcompanion.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualcompanion.userservice.dto.request.RegisterRequest;
import com.virtualcompanion.userservice.dto.request.UpdateProfileRequest;
import com.virtualcompanion.userservice.entity.*;
import com.virtualcompanion.userservice.repository.UserRepository;
import com.virtualcompanion.userservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    private String authToken;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .email("test@example.com")
                .username("testuser")
                .password("$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewiYpXO.y/oi/5.2") // "password"
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .subscriptionLevel(SubscriptionLevel.FREE)
                .roles(Set.of(UserRole.USER))
                .build();
        
        testUser = userRepository.save(testUser);
        authToken = jwtTokenProvider.generateAccessToken(testUser);
    }
    
    @Test
    void register_ValidRequest_Returns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .username("newuser")
                .password("SecurePass123!")
                .firstName("New")
                .lastName("User")
                .birthDate(LocalDate.of(1995, 5, 15))
                .acceptedTerms(true)
                .build();
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.email").value("newuser@example.com"));
    }
    
    @Test
    void getProfile_Authenticated_ReturnsProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.data.username").value(testUser.getUsername()));
    }
    
    @Test
    void updateProfile_ValidData_UpdatesSuccessfully() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .phoneNumber("+33612345678")
                .build();
        
        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Updated"))
                .andExpect(jsonPath("$.data.lastName").value("Name"));
    }
    
    @Test
    void getProfile_Unauthorized_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void getSessions_Authenticated_ReturnsSessions() throws Exception {
        mockMvc.perform(get("/api/v1/users/sessions")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    void enable2FA_Authenticated_ReturnsSetupInfo() throws Exception {
        mockMvc.perform(post("/api/v1/users/2fa/enable")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.secret").exists())
                .andExpect(jsonPath("$.data.qrCodeUrl").exists())
                .andExpect(jsonPath("$.data.backupCodes").isArray());
    }
}