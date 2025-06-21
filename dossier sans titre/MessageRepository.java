package com.virtualcompanion.conversationservice.repository;

public interface MessageRepository extends MongoRepository<Message, String> {
    
    Page<Message> findByConversationIdOrderByTimestampDesc(UUID conversationId, Pageable pageable);
    
    List<Message> findByConversationIdAndTimestampBetween(UUID conversationId, 
                                                          LocalDateTime start, 
                                                          LocalDateTime end);
    
    @Query("{ 'conversationId': ?0, 'metadata.emotion': { $exists: true } }")
    List<Message> findMessagesWithEmotions(UUID conversationId);
    
    @Query("{ 'conversationId': ?0, 'role': 'assistant', 'metadata.processingTime': { $gt: ?1 } }")
    List<Message> findSlowResponses(UUID conversationId, Long threshold);
    
    Long countByConversationId(UUID conversationId);
    
    @Query(value = "{ 'conversationId': ?0 }", delete = true)
    void deleteByConversationId(UUID conversationId);
    
    @Query("{ 'userId': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<Message> findUserMessagesByDateRange(UUID userId, LocalDateTime start, LocalDateTime end);
}
