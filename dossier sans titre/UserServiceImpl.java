package com.virtualcompanion.userservice.service.impl;

public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final UserComplianceRepository complianceRepository;
    private final VerificationTokenRepository tokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TwoFactorService twoFactorService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public UserResponse createUser(RegisterRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email already exists: " + request.getEmail());
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + request.getUsername());
        }
        
        // Create user entity
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .age(request.getAge())
                .phoneNumber(request.getPhoneNumber())
                .locale(request.getLocale() != null ? request.getLocale() : "en_US")
                .subscriptionLevel(User.SubscriptionLevel.FREE)
                .isActive(true)
                .emailVerified(false)
                .twoFactorEnabled(false)
                .build();
        
        user = userRepository.save(user);
        
        // Create default preferences
        UserPreference preferences = UserPreference.builder()
                .userId(user.getId())
                .theme("light")
                .language(user.getLocale())
                .emailNotifications(true)
                .pushNotifications(false)
                .contentFilter("MODERATE")
                .autoSaveConversations(true)
                .build();
        
        preferenceRepository.save(preferences);
        
        // Create compliance record
        UserCompliance compliance = UserCompliance.builder()
                .userId(user.getId())
                .termsAcceptedDate(LocalDateTime.now())
                .termsVersion("1.0")
                .jurisdiction(request.getJurisdiction())
                .build();
        
        complianceRepository.save(compliance);
        
        // Send verification email
        String verificationToken = generateVerificationToken(user.getId(), VerificationToken.TokenType.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationToken);
        
        // Publish user created event
        kafkaTemplate.send("user-events", "user.created", 
            new UserCreatedEvent(user.getId(), user.getEmail(), user.getUsername()));
        
        log.info("User created successfully with ID: {}", user.getId());
        return userMapper.toResponse(user);
    }
    
    @Override
    @Cacheable(value = "users", key = "#userId")
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return userMapper.toResponse(user);
    }
    
    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }
    
    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAllActiveUsers(pageable)
                .map(userMapper::toResponse);
    }
    
    @Override
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
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
        if (request.getLocale() != null) {
            user.setLocale(request.getLocale());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        
        kafkaTemplate.send("user-events", "user.updated", 
            new UserUpdatedEvent(user.getId(), request));
        
        return userMapper.toResponse(user);
    }
    
    @Override
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Soft delete
        user.setIsActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Invalidate all sessions
        sessionRepository.invalidateAllUserSessions(userId);
        
        kafkaTemplate.send("user-events", "user.deleted", 
            new UserDeletedEvent(userId));
        
        log.info("User soft deleted: {}", userId);
    }
    
    @Override
    public void verifyEmail(String verificationToken) {
        VerificationToken token = tokenRepository.findByToken(verificationToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));
        
        if (token.isUsed()) {
            throw new InvalidTokenException("Token already used");
        }
        
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token expired");
        }
        
        // Verify email
        userRepository.verifyEmail(token.getUserId());
        tokenRepository.markAsUsed(verificationToken, LocalDateTime.now());
        
        kafkaTemplate.send("user-events", "user.email.verified", 
            new EmailVerifiedEvent(token.getUserId()));
        
        log.info("Email verified for user: {}", token.getUserId());
    }
    
    @Override
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email already verified");
        }
        
        // Check if there's an active token
        long activeTokens = tokenRepository.countActiveTokensForUser(
            user.getId(), VerificationToken.TokenType.EMAIL_VERIFICATION, LocalDateTime.now());
        
        if (activeTokens > 0) {
            throw new IllegalStateException("Active verification token already exists");
        }
        
        String verificationToken = generateVerificationToken(user.getId(), VerificationToken.TokenType.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationToken);
        
        log.info("Resent verification email to: {}", email);
    }
    
    @Override
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updatePassword(UUID userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user = userRepository.save(user);
        
        // Invalidate all sessions except current
        sessionRepository.invalidateAllUserSessions(userId);
        
        kafkaTemplate.send("user-events", "user.password.changed", 
            new PasswordChangedEvent(userId));
        
        return userMapper.toResponse(user);
    }
    
    @Override
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        String resetToken = generateVerificationToken(user.getId(), VerificationToken.TokenType.PASSWORD_RESET);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetToken);
        
        log.info("Password reset requested for: {}", email);
    }
    
    @Override
    public void resetPassword(String token, String newPassword) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));
        
        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("Token already used");
        }
        
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token expired");
        }
        
        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        
        tokenRepository.markAsUsed(token, LocalDateTime.now());
        sessionRepository.invalidateAllUserSessions(user.getId());
        
        kafkaTemplate.send("user-events", "user.password.reset", 
            new PasswordResetEvent(user.getId()));
        
        log.info("Password reset for user: {}", user.getId());
    }
    
    @Override
    public UserPreferenceResponse getUserPreferences(UUID userId) {
        UserPreference preferences = preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Preferences not found for user: " + userId));
        return userMapper.toPreferenceResponse(preferences);
    }
    
    @Override
    public UserPreferenceResponse updateUserPreferences(UUID userId, UpdatePreferenceRequest request) {
        UserPreference preferences = preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Preferences not found for user: " + userId));
        
        // Update preferences
        if (request.getTheme() != null) {
            preferences.setTheme(request.getTheme());
        }
        if (request.getLanguage() != null) {
            preferences.setLanguage(request.getLanguage());
        }
        if (request.getEmailNotifications() != null) {
            preferences.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getPushNotifications() != null) {
            preferences.setPushNotifications(request.getPushNotifications());
        }
        if (request.getContentFilter() != null) {
            preferences.setContentFilter(request.getContentFilter());
        }
        if (request.getAutoSaveConversations() != null) {
            preferences.setAutoSaveConversations(request.getAutoSaveConversations());
        }
        
        preferences = preferenceRepository.save(preferences);
        
        kafkaTemplate.send("user-events", "user.preferences.updated", 
            new PreferencesUpdatedEvent(userId, request));
        
        return userMapper.toPreferenceResponse(preferences);
    }
    
    @Override
    public ComplianceStatusResponse getComplianceStatus(UUID userId) {
        UserCompliance compliance = complianceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance status not found for user: " + userId));
        return userMapper.toComplianceResponse(compliance);
    }
    
    @Override
    public void updateComplianceStatus(UUID userId, UpdateComplianceRequest request) {
        UserCompliance compliance = complianceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance status not found for user: " + userId));
        
        if (request.getGdprConsent() != null && request.getGdprConsent()) {
            compliance.setGdprConsentDate(LocalDateTime.now());
        }
        if (request.getCcpaConsent() != null && request.getCcpaConsent()) {
            compliance.setCcpaConsentDate(LocalDateTime.now());
        }
        if (request.getMarketingConsent() != null) {
            compliance.setMarketingConsentDate(request.getMarketingConsent() ? LocalDateTime.now() : null);
        }
        if (request.getDataProcessingConsent() != null) {
            compliance.setDataProcessingConsentDate(request.getDataProcessingConsent() ? LocalDateTime.now() : null);
        }
        
        complianceRepository.save(compliance);
        
        kafkaTemplate.send("user-events", "user.compliance.updated", 
            new ComplianceUpdatedEvent(userId, request));
    }
    
    @Override
    public SubscriptionResponse getUserSubscription(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        return SubscriptionResponse.builder()
                .userId(userId)
                .level(user.getSubscriptionLevel().name())
                .startDate(user.getSubscriptionStartDate())
                .endDate(user.getSubscriptionEndDate())
                .autoRenew(user.isAutoRenew())
                .build();
    }
    
    @Override
    @CacheEvict(value = "users", key = "#userId")
    public void updateSubscription(UUID userId, UpdateSubscriptionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setSubscriptionLevel(User.SubscriptionLevel.valueOf(request.getLevel()));
        user.setSubscriptionStartDate(LocalDateTime.now());
        user.setSubscriptionEndDate(request.getEndDate());
        user.setAutoRenew(request.isAutoRenew());
        
        userRepository.save(user);
        
        kafkaTemplate.send("user-events", "user.subscription.updated", 
            new SubscriptionUpdatedEvent(userId, request));
    }
    
    @Override
    public Page<UserResponse> searchUsers(String query, Pageable pageable) {
        // This would typically use Elasticsearch
        // For now, using simple database query
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }
    
    @Override
    @CacheEvict(value = "users", key = "#userId")
    public void lockUserAccount(UUID userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setAccountLocked(true);
        user.setAccountLockedReason(reason);
        user.setAccountLockedAt(LocalDateTime.now());
        userRepository.save(user);
        
        sessionRepository.invalidateAllUserSessions(userId);
        
        kafkaTemplate.send("user-events", "user.account.locked", 
            new AccountLockedEvent(userId, reason));
        
        log.info("User account locked: {} - Reason: {}", userId, reason);
    }
    
    @Override
    @CacheEvict(value = "users", key = "#userId")
    public void unlockUserAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setAccountLocked(false);
        user.setAccountLockedReason(null);
        user.setAccountLockedAt(null);
        userRepository.save(user);
        
        kafkaTemplate.send("user-events", "user.account.unlocked", 
            new AccountUnlockedEvent(userId));
        
        log.info("User account unlocked: {}", userId);
    }
    
    @Override
    public UserStatisticsResponse getUserStatistics(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        long activeSessions = sessionRepository.countActiveSessionsForUser(userId);
        
        return UserStatisticsResponse.builder()
                .userId(userId)
                .registrationDate(user.getCreatedAt())
                .lastLoginDate(user.getLastLogin())
                .totalLogins(user.getTotalLogins())
                .activeSessions(activeSessions)
                .subscriptionLevel(user.getSubscriptionLevel().name())
                .build();
    }
    
    @Override
    public void enableTwoFactorAuth(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (user.isTwoFactorEnabled()) {
            throw new IllegalStateException("Two-factor authentication already enabled");
        }
        
        String secret = twoFactorService.generateSecret();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        
        kafkaTemplate.send("user-events", "user.2fa.enabled", 
            new TwoFactorEnabledEvent(userId));
        
        log.info("Two-factor authentication enabled for user: {}", userId);
    }
    
    @Override
    public void disableTwoFactorAuth(UUID userId, String verificationCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (!user.isTwoFactorEnabled()) {
            throw new IllegalStateException("Two-factor authentication not enabled");
        }
        
        // Verify code before disabling
        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), verificationCode)) {
            throw new UnauthorizedException("Invalid verification code");
        }
        
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
        
        kafkaTemplate.send("user-events", "user.2fa.disabled", 
            new TwoFactorDisabledEvent(userId));
        
        log.info("Two-factor authentication disabled for user: {}", userId);
    }
    
    @Override
    public String generateTwoFactorQrCode(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (!user.isTwoFactorEnabled()) {
            throw new IllegalStateException("Two-factor authentication not enabled");
        }
        
        return twoFactorService.generateQrCodeUrl(user.getEmail(), user.getTwoFactorSecret());
    }
    
    @Override
    public boolean verifyTwoFactorCode(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (!user.isTwoFactorEnabled()) {
            return true; // 2FA not enabled, so verification passes
        }
        
        return twoFactorService.verifyCode(user.getTwoFactorSecret(), code);
    }
    
    private String generateVerificationToken(UUID userId, VerificationToken.TokenType type) {
        String token = PasswordGenerator.generateToken();
        
        VerificationToken verificationToken = VerificationToken.builder()
                .userId(userId)
                .token(token)
                .type(type)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        
        tokenRepository.save(verificationToken);
        
        return token;
    }
}
