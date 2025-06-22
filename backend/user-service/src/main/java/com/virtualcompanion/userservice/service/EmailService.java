package com.virtualcompanion.userservice.service;

public interface EmailService {
    void sendVerificationEmail(String email, String name, String verificationToken);

    void sendPasswordResetEmail(String email, String name, String resetToken);

    void sendWelcomeEmail(String email, String name);

    void sendAccountLockedEmail(String email, String name, String reason);

    void sendSubscriptionConfirmationEmail(String email, String name, String subscriptionLevel);

    void sendSubscriptionExpiryReminderEmail(String email, String name, int daysRemaining);
}
