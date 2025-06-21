package com.virtualcompanion.userservice.repository;

public interface TwoFactorService {
    TwoFactorSetupResponse setupTwoFactor(UUID userId);
    void confirmTwoFactor(UUID userId, String code);
    void disableTwoFactor(UUID userId, String password);
    boolean verifyCode(User user, String code);
    String generateBackupCodes(User user);
}
