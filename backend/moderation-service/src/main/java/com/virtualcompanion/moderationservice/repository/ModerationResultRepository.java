package com.virtualcompanion.moderationservice.repository;

public interface ModerationResultRepository extends JpaRepository<ModerationResult, UUID> {
    
    Optional<ModerationResult> findByModerationRequestId(UUID requestId);
    
    List<ModerationResult> findByModeratorIdAndCreatedAtBetween(UUID moderatorId, 
                                                                LocalDateTime start, 
                                                                LocalDateTime end);
    
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (mr.createdAt - req.createdAt))) FROM ModerationResult mr " +
           "JOIN mr.moderationRequest req WHERE mr.createdAt >= :since")
    Double getAverageResponseTime(@Param("since") LocalDateTime since);
    
    @Query("SELECT mr.decision, COUNT(mr) FROM ModerationResult mr WHERE mr.createdAt >= :since " +
           "GROUP BY mr.decision")
    List<Object[]> getDecisionDistribution(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(mr) FROM ModerationResult mr WHERE mr.decisionType = 'automated' " +
           "AND mr.createdAt >= :since")
    Long countAutomatedDecisions(@Param("since") LocalDateTime since);
}
