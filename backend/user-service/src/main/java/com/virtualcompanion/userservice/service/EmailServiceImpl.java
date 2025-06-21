package com.virtualcompanion.userservice.service;

public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.email.from-name}")
    private String fromName;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    @Override
    public void sendVerificationEmail(String email, String name, String verificationToken) {
        try {
            String verificationUrl = frontendUrl + "/auth/verify-email?token=" + verificationToken;
            
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("verificationUrl", verificationUrl);
            
            String htmlContent = templateEngine.process("email/verification", context);
            
            sendHtmlEmail(
                email,
                "Verify your Virtual Companion account",
                htmlContent
            );
            
            publishEmailEvent("verification", email);
            
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", email, e.getMessage());
        }
    }
    
    @Override
    public void sendPasswordResetEmail(String email, String name, String resetToken) {
        try {
            String resetUrl = frontendUrl + "/auth/reset-password?token=" + resetToken;
            
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("resetUrl", resetUrl);
            
            String htmlContent = templateEngine.process("email/password-reset", context);
            
            sendHtmlEmail(
                email,
                "Reset your Virtual Companion password",
                htmlContent
            );
            
            publishEmailEvent("password-reset", email);
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
        }
    }
    
    @Override
    public void sendWelcomeEmail(String email, String name) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");
            
            String htmlContent = templateEngine.process("email/welcome", context);
            
            sendHtmlEmail(
                email,
                "Welcome to Virtual Companion!",
                htmlContent
            );
            
            publishEmailEvent("welcome", email);
            
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", email, e.getMessage());
        }
    }
    
    @Override
    public void sendAccountLockedEmail(String email, String name, String reason) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("reason", reason);
            context.setVariable("supportUrl", frontendUrl + "/support");
            
            String htmlContent = templateEngine.process("email/account-locked", context);
            
            sendHtmlEmail(
                email,
                "Your Virtual Companion account has been locked",
                htmlContent
            );
            
            publishEmailEvent("account-locked", email);
            
        } catch (Exception e) {
            log.error("Failed to send account locked email to {}: {}", email, e.getMessage());
        }
    }
    
    @Override
    public void sendSubscriptionConfirmationEmail(String email, String name, String subscriptionLevel) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("subscriptionLevel", subscriptionLevel);
            context.setVariable("billingUrl", frontendUrl + "/billing");
            
            String htmlContent = templateEngine.process("email/subscription-confirmation", context);
            
            sendHtmlEmail(
                email,
                "Your Virtual Companion subscription is active",
                htmlContent
            );
            
            publishEmailEvent("subscription-confirmation", email);
            
        } catch (Exception e) {
            log.error("Failed to send subscription confirmation email to {}: {}", email, e.getMessage());
        }
    }
    
    @Override
    public void sendSubscriptionExpiryReminderEmail(String email, String name, int daysRemaining) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("daysRemaining", daysRemaining);
            context.setVariable("renewUrl", frontendUrl + "/billing/renew");
            
            String htmlContent = templateEngine.process("email/subscription-expiry-reminder", context);
            
            sendHtmlEmail(
                email,
                "Your Virtual Companion subscription is expiring soon",
                htmlContent
            );
            
            publishEmailEvent("subscription-expiry-reminder", email);
            
        } catch (Exception e) {
            log.error("Failed to send subscription expiry reminder to {}: {}", email, e.getMessage());
        }
    }
    
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
        
        log.info("Email sent successfully to: {}", to);
    }
    
    private void publishEmailEvent(String type, String recipient) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("recipient", recipient);
        event.put("timestamp", System.currentTimeMillis());
        
        kafkaTemplate.send("email-events", "email.sent", event);
    }
}
