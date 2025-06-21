package com.virtualcompanion.userservice.repository;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    
    Optional<UserSession> findBySessionToken(String sessionToken);
    
    List<UserSession> findByUserIdAndRevokedFalseOrderByCreatedAtDesc(UUID userId);
    
    @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId AND s.revoked = false AND s.expiresAt > :now")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.revoked = true WHERE s.user.id = :userId AND s.sessionToken != :currentToken")
    void revokeAllUserSessionsExcept(@Param("userId") UUID userId, @Param("currentToken") String currentToken);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivityAt = :now WHERE s.sessionToken = :sessionToken")
    void updateLastActivity(@Param("sessionToken") String sessionToken, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now OR s.revoked = true")
    void deleteExpiredAndRevokedSessions(@Param("now") LocalDateTime now);
}
