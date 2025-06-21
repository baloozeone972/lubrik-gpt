package com.virtualcompanion.userservice.repository;

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
