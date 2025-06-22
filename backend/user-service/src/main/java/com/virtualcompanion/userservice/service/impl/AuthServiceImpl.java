package com.virtualcompanion.userservice.service.impl;

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
