package com.virtualcompanion.conversationservice.repository;

public interface ConversationAnalyticsRepository extends JpaRepository<ConversationAnalytics, UUID> {

    Optional<ConversationAnalytics> findByConversationId(UUID conversationId);

    @Query("SELECT ca FROM ConversationAnalytics ca WHERE ca.userId = :userId " +
            "AND ca.createdAt >= :startDate AND ca.createdAt <= :endDate")
    List<ConversationAnalytics> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT AVG(ca.sentimentScore) FROM ConversationAnalytics ca WHERE ca.userId = :userId")
    Double getAverageSentimentByUser(@Param("userId") UUID userId);

    @Query("SELECT ca.dominantEmotion, COUNT(ca) FROM ConversationAnalytics ca " +
            "WHERE ca.userId = :userId GROUP BY ca.dominantEmotion")
    List<Object[]> getEmotionDistributionByUser(@Param("userId") UUID userId);

    @Query("SELECT AVG(ca.responseTime) FROM ConversationAnalytics ca WHERE ca.characterId = :characterId")
    Double getAverageResponseTimeByCharacter(@Param("characterId") UUID characterId);

    @Query("SELECT DATE(ca.createdAt), COUNT(ca), AVG(ca.engagementScore) " +
            "FROM ConversationAnalytics ca WHERE ca.userId = :userId " +
            "GROUP BY DATE(ca.createdAt) ORDER BY DATE(ca.createdAt) DESC")
    List<Object[]> getDailyEngagementMetrics(@Param("userId") UUID userId);
}
