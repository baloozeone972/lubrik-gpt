package com.virtualcompanion.conversationservice.repository;

public interface StreamingSessionRepository extends JpaRepository<StreamingSession, UUID> {
    
    Optional<StreamingSession> findBySessionIdAndUserId(String sessionId, UUID userId);
    
    List<StreamingSession> findByUserIdAndIsActiveTrue(UUID userId);
    
    List<StreamingSession> findByConversationIdAndIsActiveTrue(UUID conversationId);
    
    @Modifying
    @Query("UPDATE StreamingSession s SET s.isActive = false, s.disconnectedAt = :now WHERE s.sessionId = :sessionId")
    void closeSession(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE StreamingSession s SET s.lastPingAt = :now WHERE s.sessionId = :sessionId")
    void updatePing(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM StreamingSession s WHERE s.isActive = true AND s.lastPingAt < :threshold")
    List<StreamingSession> findStaleSessions(@Param("threshold") LocalDateTime threshold);
    
    void deleteByIsActiveFalseAndDisconnectedAtBefore(LocalDateTime threshold);
}
