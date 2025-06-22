package com.virtualcompanion.conversationservice.repository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    Page<Conversation> findByUserIdOrderByLastActivityAtDesc(UUID userId, Pageable pageable);

    Page<Conversation> findByUserIdAndCharacterIdOrderByLastActivityAtDesc(UUID userId, UUID characterId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId AND c.status = :status")
    List<Conversation> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);

    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId AND c.lastActivityAt >= :since")
    List<Conversation> findRecentConversations(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE Conversation c SET c.lastActivityAt = :now, c.messageCount = c.messageCount + 1 WHERE c.id = :id")
    void updateActivity(@Param("id") UUID id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Conversation c SET c.status = :status, c.endedAt = :endedAt WHERE c.id = :id")
    void endConversation(@Param("id") UUID id, @Param("status") String status, @Param("endedAt") LocalDateTime endedAt);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.userId = :userId AND c.characterId = :characterId")
    long countUserCharacterConversations(@Param("userId") UUID userId, @Param("characterId") UUID characterId);

    @Query("SELECT AVG(c.messageCount) FROM Conversation c WHERE c.userId = :userId")
    Double getAverageMessageCountByUser(@Param("userId") UUID userId);

    @Query("SELECT c FROM Conversation c WHERE c.status = 'active' AND c.lastActivityAt < :threshold")
    List<Conversation> findInactiveConversations(@Param("threshold") LocalDateTime threshold);
}
