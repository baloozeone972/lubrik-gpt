package com.virtualcompanion.userservice.repository;

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
