package com.virtualcompanion.conversationservice.repository;

public interface ConversationMemoryRepository extends JpaRepository<ConversationMemory, UUID> {
    
    Optional<ConversationMemory> findByConversationIdAndUserId(UUID conversationId, UUID userId);
    
    List<ConversationMemory> findByUserIdAndCharacterIdOrderByLastUpdatedDesc(UUID userId, UUID characterId);
    
    @Query(value = "SELECT * FROM conversation_memories WHERE user_id = :userId AND character_id = :characterId " +
                   "ORDER BY embedding <-> :queryEmbedding LIMIT :limit", nativeQuery = true)
    List<ConversationMemory> findSimilarMemories(@Param("userId") UUID userId, 
                                                 @Param("characterId") UUID characterId,
                                                 @Param("queryEmbedding") float[] queryEmbedding,
                                                 @Param("limit") int limit);
    
    @Query("SELECT cm FROM ConversationMemory cm WHERE cm.userId = :userId AND cm.characterId = :characterId " +
           "AND cm.memoryType = :type ORDER BY cm.importance DESC")
    List<ConversationMemory> findByTypeAndImportance(@Param("userId") UUID userId,
                                                     @Param("characterId") UUID characterId,
                                                     @Param("type") String type);
    
    void deleteByConversationId(UUID conversationId);
}
