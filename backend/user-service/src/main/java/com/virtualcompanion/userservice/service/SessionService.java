package com.virtualcompanion.userservice.service;

public interface SessionService {
    UserSession createSession(User user, LoginRequest loginRequest, HttpServletRequest request);
    List<SessionResponse> getUserSessions(UUID userId);
    void revokeSession(UUID userId, UUID sessionId);
    void revokeAllSessions(UUID userId, String exceptToken);
    void updateSessionActivity(String sessionToken);
    void cleanupExpiredSessions();
}
