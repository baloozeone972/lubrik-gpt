package com.virtualcompanion.conversationservice.service;

public interface MemoryService {
    void extractAndStoreMemory(Conversation conversation, Message userMessage, Message aiMessage);
    void consolidateConversationMemory(UUID userId, UUID conversationId);
    void updateCharacterMemory(UUID userId, UUID characterId, List<MemoryItem> memories);
    List<String> retrieveRelevantMemories(UUID userId, UUID characterId, String query, int limit);
}
