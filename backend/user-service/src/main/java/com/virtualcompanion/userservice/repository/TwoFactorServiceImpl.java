package com.virtualcompanion.userservice.repository;

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
