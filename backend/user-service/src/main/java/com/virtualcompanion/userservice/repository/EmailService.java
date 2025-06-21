package com.virtualcompanion.userservice.repository;

public interface EmailService {
    void sendVerificationEmail(User user, String token);
    void sendPasswordResetEmail(User user, String token);
    void sendWelcomeEmail(User user);
    void sendPasswordChangedEmail(User user);
    void sendAccountLockedEmail(User user);
    void sendDataExportEmail(User user, String downloadUrl);
    void sendAccountDeletionEmail(User user);
}
