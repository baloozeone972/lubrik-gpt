package com.virtualcompanion.mediaservice.repository;

public interface StreamingSessionRepository extends JpaRepository<StreamingSession, UUID> {

    Optional<StreamingSession> findBySessionId(String sessionId);

    List<StreamingSession> findByUserIdAndStatus(UUID userId, String status);

    List<StreamingSession> findByCharacterIdAndStatus(UUID characterId, String status);

    @Query("SELECT s FROM StreamingSession s WHERE s.status = 'active' AND s.startedAt < :threshold")
    List<StreamingSession> findLongRunningSessions(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("UPDATE StreamingSession s SET s.status = :status, s.endedAt = :endedAt WHERE s.sessionId = :sessionId")
    void endSession(@Param("sessionId") String sessionId, @Param("status") String status, @Param("endedAt") LocalDateTime endedAt);

    @Modifying
    @Query("UPDATE StreamingSession s SET s.sdpAnswer = :sdpAnswer WHERE s.sessionId = :sessionId")
    void updateSdpAnswer(@Param("sessionId") String sessionId, @Param("sdpAnswer") String sdpAnswer);

    @Query("SELECT COUNT(s) FROM StreamingSession s WHERE s.userId = :userId AND s.status = 'active'")
    long countActiveSessionsByUser(@Param("userId") UUID userId);
}
