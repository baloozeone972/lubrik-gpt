// AuthService.java
package com.virtualcompanion.userservice.service;

import com.virtualcompanion.userservice.dto.*;

public interface AuthService {
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);
    void logout(String token);
    AuthResponse refreshToken(String refreshToken);
    boolean isEmailAvailable(String email);
    boolean isUsernameAvailable(String username);
    TokenValidationResponse validateToken(String token);
}

// AuthServiceImpl.java
package com.virtualcompanion.userservice.service.impl;

import com.virtualcompanion.userservice.dto.*;
import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.entity.UserSession;
import com.virtualcompanion.userservice.exception.UnauthorizedException;
import com.virtualcompanion.userservice.exception.ResourceNotFoundException;
import com.virtualcompanion.userservice.repository.UserRepository;
import com.virtualcompanion.userservice.repository.UserSessionRepository;
import com.virtualcompanion.userservice.security.JwtTokenProvider;
import com.virtualcompanion.userservice.service.AuthService;
import com.virtualcompanion.userservice.service.TwoFactorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final TwoFactorService twoFactorService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            User user = (User) authentication.getPrincipal();
            
            // Check if account is locked
            if (user.isAccountLocked()) {
                throw new UnauthorizedException("Account is locked: " + user.getAccountLockedReason());
            }
            
            // Verify 2FA if enabled
            if (user.isTwoFactorEnabled()) {
                if (request.getTwoFactorCode() == null || request.getTwoFactorCode().isEmpty()) {
                    return AuthResponse.builder()
                            .requiresTwoFactor(true)
                            .build();
                }
                
                if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), request.getTwoFactorCode())) {
                    throw new UnauthorizedException("Invalid two-factor authentication code");
                }
            }
            
            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);
            
            // Create session
            UserSession session = UserSession.builder()
                    .userId(user.getId())
                    .sessionToken(refreshToken)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .deviceType(detectDeviceType(userAgent))
                    .createdAt(LocalDateTime.now())
                    .lastActivityAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .isActive(true)
                    .build();
            
            sessionRepository.save(session);
            
            // Update user last login
            userRepository.updateLastLogin(user.getId(), LocalDateTime.now());
            
            // Publish login event
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("userId", user.getId());
            eventData.put("ipAddress", ipAddress);
            eventData.put("userAgent", userAgent);
            eventData.put("timestamp", LocalDateTime.now());
            
            kafkaTemplate.send("user-events", "user.login", eventData);
            
            log.info("User logged in successfully: {}", user.getEmail());
            
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .roles(user.getRoles())
                    .requiresTwoFactor(false)
                    .build();
            
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }
    }
    
    @Override
    public void logout(String token) {
        String sessionToken = jwtTokenProvider.getRefreshTokenFromAccessToken(token);
        
        UserSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        
        session.setIsActive(false);
        session.setLoggedOutAt(LocalDateTime.now());
        sessionRepository.save(session);
        
        // Publish logout event
        kafkaTemplate.send("user-events", "user.logout", 
            Map.of("userId", session.getUserId(), "sessionId", session.getId()));
        
        log.info("User logged out, session invalidated: {}", session.getId());
    }
    
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        // Validate refresh token
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        
        // Find active session
        UserSession session = sessionRepository.findBySessionToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Session not found"));
        
        if (!session.getIsActive()) {
            throw new UnauthorizedException("Session is inactive");
        }
        
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Session expired");
        }
        
        // Get user
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        
        // Update session activity
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(user.getRoles())
                .requiresTwoFactor(false)
                .build();
    }
    
    @Override
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
    
    @Override
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
    
    @Override
    public TokenValidationResponse validateToken(String token) {
        try {
            boolean isValid = jwtTokenProvider.validateAccessToken(token);
            
            if (isValid) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                long expirationTime = jwtTokenProvider.getExpirationTimeFromToken(token);
                
                return TokenValidationResponse.builder()
                        .valid(true)
                        .userId(userId)
                        .username(username)
                        .expiresAt(expirationTime)
                        .build();
            }
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
        }
        
        return TokenValidationResponse.builder()
                .valid(false)
                .build();
    }
    
    private String detectDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        userAgent = userAgent.toLowerCase();
        
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "Mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }
}

// EmailService.java
package com.virtualcompanion.userservice.service;

public interface EmailService {
    void sendVerificationEmail(String email, String name, String verificationToken);
    void sendPasswordResetEmail(String email, String name, String resetToken);
    void sendWelcomeEmail(String email, String name);
    void sendAccountLockedEmail(String email, String name, String reason);
    void sendSubscriptionConfirmationEmail(String email, String name, String subscriptionLevel);
    void sendSubscriptionExpiryReminderEmail(String email, String name, int daysRemaining);
}

// EmailServiceImpl.java
package com.virtualcompanion.userservice.service.impl;

import com.virtualcompanion.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
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

// TwoFactorService.java
package com.virtualcompanion.userservice.service;

public interface TwoFactorService {
    String generateSecret();
    String generateQrCodeUrl(String email, String secret);
    boolean verifyCode(String secret, String code);
}

// TwoFactorServiceImpl.java
package com.virtualcompanion.userservice.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.virtualcompanion.userservice.service.TwoFactorService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
public class TwoFactorServiceImpl implements TwoFactorService {
    
    private final GoogleAuthenticator googleAuthenticator;
    
    @Value("${app.name}")
    private String appName;
    
    public TwoFactorServiceImpl() {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setCodeDigits(6)
                .setTimeStepSizeInMillis(30000)
                .setWindowSize(3)
                .build();
                
        this.googleAuthenticator = new GoogleAuthenticator(config);
    }
    
    @Override
    public String generateSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }
    
    @Override
    public String generateQrCodeUrl(String email, String secret) {
        String otpAuthUrl = String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            appName,
            email,
            secret,
            appName
        );
        
        try {
            return generateQrCodeDataUrl(otpAuthUrl);
        } catch (Exception e) {
            log.error("Failed to generate QR code for user {}: {}", email, e.getMessage());
            return otpAuthUrl; // Return the URL as fallback
        }
    }
    
    @Override
    public boolean verifyCode(String secret, String code) {
        try {
            int codeInt = Integer.parseInt(code);
            return googleAuthenticator.authorize(secret, codeInt);
        } catch (NumberFormatException e) {
            log.error("Invalid code format: {}", code);
            return false;
        }
    }
    
    private String generateQrCodeDataUrl(String text) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        byte[] qrCodeBytes = outputStream.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(qrCodeBytes);
        
        return "data:image/png;base64," + base64;
    }
}