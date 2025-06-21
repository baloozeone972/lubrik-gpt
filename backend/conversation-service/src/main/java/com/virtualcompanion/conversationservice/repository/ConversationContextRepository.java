package com.virtualcompanion.conversationservice.repository;

public interface ConversationContextRepository extends MongoRepository<ConversationContext, String> {
    
    Optional<ConversationContext> findByConversationId(UUID conversationId);
    
    @Query("{ 'conversationId': ?0, 'isActive': true }")
    Optional<ConversationContext> findActiveContext(UUID conversationId);
    
    void deleteByConversationId(UUID conversationId);
}
