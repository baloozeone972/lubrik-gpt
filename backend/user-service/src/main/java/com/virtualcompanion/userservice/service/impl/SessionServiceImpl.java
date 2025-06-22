package com.virtualcompanion.userservice.service.impl;

public class SessionServiceImpl implements SessionService {
    
    private final UserSessionRepository sessionRepository;
    private final GeoLocationService geoLocationService;
    
    @Value("${app.jwt.expiration-hours}")
    private int sessionExpirationHours;
    
    @Override
    public UserSession createSession(User user, LoginRequest loginRequest, HttpServletRequest request) {
        String ipAddress = IpUtils.getClientIp(request);
        String userAgentString = request.getHeader("User-Agent");
        UserAgent userAgent = UserAgent.parseUserAgentString(userAgentString);
        
        // Get location from IP
        GeoLocation location = geoLocationService.getLocationFromIp(ipAddress);
        
        UserSession session = UserSession.builder()
                .user(user)
                .sessionToken(UUID.randomUUID().toString())
                .refreshToken(UUID.randomUUID().toString())
                .deviceId(loginRequest.getDeviceId())
                .deviceType(userAgent.getOperatingSystem().getDeviceType().getName())
                .deviceName(loginRequest.getDeviceName() != null ? loginRequest.getDeviceName() : userAgent.getBrowser().getName())
                .ipAddress(ipAddress)
                .userAgent(userAgentString)
                .locationCountry(location.getCountryCode())
                .locationCity(location.getCity())
                .expiresAt(LocalDateTime.now().plusHours(sessionExpirationHours))
                .build();
        
        return sessionRepository.save(session);
    }
    
    @Override
    public List<SessionResponse> getUserSessions(UUID userId) {
        List<UserSession> sessions = sessionRepository.findActiveSessionsByUserId(userId, LocalDateTime.now());
        
        return sessions.stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void revokeSession(UUID userId, UUID sessionId) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        
        if (!session.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Cannot revoke session of another user");
        }
        
        session.setRevoked(true);
        sessionRepository.save(session);
    }
    
    @Override
    public void revokeAllSessions(UUID userId, String exceptToken) {
        if (exceptToken != null) {
            sessionRepository.revokeAllUserSessionsExcept(userId, exceptToken);
        } else {
            List<UserSession> sessions = sessionRepository.findByUserIdAndRevokedFalseOrderByCreatedAtDesc(userId);
            sessions.forEach(session -> {
                session.setRevoked(true);
                sessionRepository.save(session);
            });
        }
    }
    
    @Override
    public void updateSessionActivity(String sessionToken) {
        sessionRepository.updateLastActivity(sessionToken, LocalDateTime.now());
    }
    
    @Override
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void cleanupExpiredSessions() {
        log.info("Starting expired sessions cleanup");
        sessionRepository.deleteExpiredAndRevokedSessions(LocalDateTime.now());
        log.info("Expired sessions cleanup completed");
    }
    
    private SessionResponse mapToSessionResponse(UserSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .deviceId(session.getDeviceId())
                .deviceType(session.getDeviceType())
                .deviceName(session.getDeviceName())
                .ipAddress(session.getIpAddress())
                .locationCountry(session.getLocationCountry())
                .locationCity(session.getLocationCity())
                .createdAt(session.getCreatedAt())
                .lastActivityAt(session.getLastActivityAt())
                .expiresAt(session.getExpiresAt())
                .current(false) // Would be set by controller based on current token
                .build();
    }
}
