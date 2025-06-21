// ===== SERVICES =====

// AuthService.java
package com.virtualcompanion.userservice.service;

import com.virtualcompanion.userservice.dto.request.*;
import com.virtualcompanion.userservice.dto.response.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);
    AuthResponse loginWith2FA(LoginRequest request, HttpServletRequest httpRequest);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(String token);
    void requestPasswordReset(PasswordResetRequest request);
    void resetPassword(ResetPasswordRequest request);
}

// AuthServiceImpl.java
package com.virtualcompanion.userservice.service.impl;

import com.virtualcompanion.userservice.dto.request.*;
import com.virtualcompanion.userservice.dto.response.AuthResponse;
import com.virtualcompanion.userservice.dto.response.UserResponse;
import com.virtualcompanion.userservice.entity.*;
import com.virtualcompanion.userservice.exception.*;
import com.virtualcompanion.userservice.mapper.UserMapper;
import com.virtualcompanion.userservice.repository.*;
import com.virtualcompanion.userservice.security.JwtTokenProvider;
import com.virtualcompanion.userservice.service.AuthService;
import com.virtualcompanion.userservice.service.EmailService;
import com.virtualcompanion.userservice.service.SessionService;
import com.virtualcompanion.userservice.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final VerificationTokenRepository tokenRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final SessionService sessionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        // Check if email or username already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already taken");
        }
        
        // Create new user
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
                .marketingConsent(request.getMarketingConsent() != null ? request.getMarketingConsent() : false)
                .build();
        
        user.setCompliance(compliance);
        user = userRepository.save(user);
        
        // Send verification email
        emailService.sendVerificationEmail(user);
        
        // Publish event
        kafkaTemplate.send("user-events", UserEvent.builder()
                .eventType("USER_REGISTERED")
                .userId(user.getId())
                .timestamp(LocalDateTime.now())
                .build());
        
        // Create session
        String ipAddress = IpUtils.getClientIp(httpRequest);
        UserSession session = sessionService.createSession(user, request, httpRequest);
        
        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, session.getSessionToken());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(userMapper.toResponse(user))
                .build();
    }
    
    @Override
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        
        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AccountLockedException("Account is locked until " + user.getLockedUntil());
        }
        
        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            // Reset failed attempts
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            
            // Check if 2FA is enabled
            if (user.isTwoFaEnabled()) {
                return AuthResponse.builder()
                        .requiresTwoFactor(true)
                        .build();
            }
            
            // Update login info
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(IpUtils.getClientIp(httpRequest));
            userRepository.save(user);
            
            // Create session
            UserSession session = sessionService.createSession(user, request, httpRequest);
            
            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user, session.getSessionToken());
            
            // Publish event
            kafkaTemplate.send("user-events", UserEvent.builder()
                    .eventType("USER_LOGIN")
                    .userId(user.getId())
                    .timestamp(LocalDateTime.now())
                    .metadata(Map.of("ip", IpUtils.getClientIp(httpRequest)))
                    .build());
            
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationTime())
                    .user(userMapper.toResponse(user))
                    .build();
            
        } catch (BadCredentialsException e) {
            // Increment failed attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            
            // Lock account if too many failed attempts
            if (user.getFailedLoginAttempts() >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                emailService.sendAccountLockedEmail(user);
            }
            
            userRepository.save(user);
            throw e;
        }
    }
    
    @Override
    public AuthResponse loginWith2FA(LoginRequest request, HttpServletRequest httpRequest) {
        if (request.getTwoFactorCode() == null || request.getTwoFactorCode().isEmpty()) {
            throw new BadCredentialsException("2FA code is required");
        }
        
        // First authenticate with password
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        
        // Verify 2FA code
        if (!twoFactorService.verifyCode(user, request.getTwoFactorCode())) {
            throw new Invalid2FACodeException("Invalid 2FA code");
        }
        
        // Continue with normal login flow
        return completeLogin(user, request, httpRequest);
    }
    
    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        
        String sessionToken = jwtTokenProvider.getSessionTokenFromJwt(refreshToken);
        UserSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new InvalidTokenException("Session not found"));
        
        if (session.isRevoked() || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Session expired or revoked");
        }
        
        User user = session.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        
        // Update session activity
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);
        
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(userMapper.toResponse(user))
                .build();
    }
    
    @Override
    public void logout(String token) {
        String jwt = token.replace("Bearer ", "");
        String sessionToken = jwtTokenProvider.getSessionTokenFromJwt(jwt);
        
        sessionRepository.findBySessionToken(sessionToken).ifPresent(session -> {
            session.setRevoked(true);
            sessionRepository.save(session);
        });
    }
    
    @Override
    public void requestPasswordReset(PasswordResetRequest request) {
        userRepository.findByEmail(request.getEmail().toLowerCase()).ifPresent(user -> {
            // Generate reset token
            String resetToken = UUID.randomUUID().toString();
            
            VerificationToken token = VerificationToken.builder()
                    .user(user)
                    .token(resetToken)
                    .tokenType(TokenType.PASSWORD_RESET)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            
            tokenRepository.save(token);
            
            // Send email
            emailService.sendPasswordResetEmail(user, resetToken);
        });
        
        // Don't reveal if email exists or not
    }
    
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }
        
        VerificationToken token = tokenRepository.findByTokenAndTokenType(
                request.getToken(), TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));
        
        if (token.isExpired() || token.isUsed()) {
            throw new InvalidTokenException("Token is expired or already used");
        }
        
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Mark token as used
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
        
        // Revoke all sessions
        sessionService.revokeAllSessions(user.getId(), null);
        
        // Send confirmation email
        emailService.sendPasswordChangedEmail(user);
    }
    
    private AuthResponse completeLogin(User user, LoginRequest request, HttpServletRequest httpRequest) {
        // Update login info
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(IpUtils.getClientIp(httpRequest));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        
        // Create session
        UserSession session = sessionService.createSession(user, request, httpRequest);
        
        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user, session.getSessionToken());
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(userMapper.toResponse(user))
                .build();
    }
}

// ===== CONFIGURATION DE SÉCURITÉ =====

// SecurityConfig.java
package com.virtualcompanion.userservice.config;

import com.virtualcompanion.userservice.security.JwtAuthenticationEntryPoint;
import com.virtualcompanion.userservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                
                // Admin endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            );
        
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3001"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

// JwtTokenProvider.java
package com.virtualcompanion.userservice.security;

import com.virtualcompanion.userservice.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration-hours}")
    private int jwtExpirationHours;
    
    @Value("${app.jwt.refresh-expiration-days}")
    private int refreshExpirationDays;
    
    @Value("${app.jwt.issuer}")
    private String issuer;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationHours * 3600 * 1000L);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("username", user.getUsername());
        claims.put("roles", user.getRoles());
        claims.put("type", "access");
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer(issuer)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    public String generateRefreshToken(User user, String sessionToken) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationDays * 24 * 3600 * 1000L);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("sessionToken", sessionToken);
        claims.put("type", "refresh");
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer(issuer)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    public UUID getUserIdFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return UUID.fromString(claims.getSubject());
    }
    
    public String getSessionTokenFromJwt(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.get("sessionToken", String.class);
    }
    
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
    
    public long getExpirationTime() {
        return jwtExpirationHours * 3600 * 1000L;
    }
}