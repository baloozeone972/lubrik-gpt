package com.virtualcompanion.conversationservice.service;

public class MemoryServiceImpl implements MemoryService {
    
    private final ConversationMemoryRepository memoryRepository;
    private final CharacterContextRepository characterContextRepository;
    private final MessageRepository messageRepository;
    private final EmbeddingService embeddingService;
    
    @Override
    public void extractAndStoreMemory(Conversation conversation, Message userMessage, Message aiMessage) {
        try {
            // Extract key information from the exchange
            String memoryContent = extractMemoryContent(userMessage.getContent(), aiMessage.getContent());
            
            if (memoryContent != null && !memoryContent.isEmpty()) {
                // Generate embedding
                float[] embedding = embeddingService.generateEmbedding(memoryContent);
                
                // Store memory
                ConversationMemory memory = ConversationMemory.builder()
                        .conversationId(conversation.getId())
                        .userId(conversation.getUserId())
                        .characterId(conversation.getCharacterId())
                        .content(memoryContent)
                        .embedding(embedding)
                        .memoryType(determineMemoryType(memoryContent))
                        .importance(calculateImportance(memoryContent))
                        .sourceMessageId(userMessage.getId())
                        .createdAt(LocalDateTime.now())
                        .lastAccessed(LocalDateTime.now())
                        .build();
                
                memoryRepository.save(memory);
                
                // Update character context
                updateCharacterContextMemory(conversation.getUserId(), 
                                           conversation.getCharacterId(), 
                                           memoryContent);
                
                log.info("Stored memory for conversation {}: {}", conversation.getId(), memoryContent);
            }
        } catch (Exception e) {
            log.error("Failed to extract and store memory: {}", e.getMessage());
        }
    }
    
    @Override
    public void consolidateConversationMemory(UUID userId, UUID conversationId) {
        try {
            // Get all messages from the conversation
            List<Message> messages = messageRepository
                    .findByConversationIdOrderByTimestampDesc(conversationId, PageRequest.of(0, 1000))
                    .getContent();
            
            // Extract important memories
            List<String> importantMemories = new ArrayList<>();
            
            for (int i = 0; i < messages.size() - 1; i++) {
                Message current = messages.get(i);
                Message next = messages.get(i + 1);
                
                if ("user".equals(current.getRole()) && "assistant".equals(next.getRole())) {
                    String memory = extractMemoryContent(current.getContent(), next.getContent());
                    if (memory != null && calculateImportance(memory) > 0.7) {
                        importantMemories.add(memory);
                    }
                }
            }
            
            // Update character context with consolidated memories
            if (!importantMemories.isEmpty()) {
                characterContextRepository.findByUserIdAndCharacterId(userId, conversationId)
                        .ifPresent(context -> {
                            context.getSharedMemories().addAll(importantMemories);
                            context.setLastUpdated(LocalDateTime.now());
                            characterContextRepository.save(context);
                        });
            }
            
            log.info("Consolidated {} memories from conversation {}", importantMemories.size(), conversationId);
            
        } catch (Exception e) {
            log.error("Failed to consolidate conversation memory: {}", e.getMessage());
        }
    }
    
    @Override
    public void updateCharacterMemory(UUID userId, UUID characterId, List<MemoryItem> memories) {
        for (MemoryItem item : memories) {
            try {
                float[] embedding = embeddingService.generateEmbedding(item.getContent());
                
                ConversationMemory memory = ConversationMemory.builder()
                        .userId(userId)
                        .characterId(characterId)
                        .content(item.getContent())
                        .embedding(embedding)
                        .memoryType(item.getType())
                        .importance(item.getImportance() != null ? item.getImportance() : 0.5)
                        .metadata(item.getMetadata())
                        .createdAt(LocalDateTime.now())
                        .lastAccessed(LocalDateTime.now())
                        .build();
                
                memoryRepository.save(memory);
            } catch (Exception e) {
                log.error("Failed to update character memory: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public List<String> retrieveRelevantMemories(UUID userId, UUID characterId, String query, int limit) {
        try {
            // Generate query embedding
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            
            // Find similar memories
            List<ConversationMemory> relevantMemories = memoryRepository
                    .findSimilarMemories(userId, characterId, queryEmbedding, limit);
            
            // Update access time
            relevantMemories.forEach(memory -> {
                memory.setLastAccessed(LocalDateTime.now());
                memory.setAccessCount(memory.getAccessCount() + 1);
            });
            memoryRepository.saveAll(relevantMemories);
            
            return relevantMemories.stream()
                    .map(ConversationMemory::getContent)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Failed to retrieve relevant memories: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private String extractMemoryContent(String userMessage, String aiResponse) {
        // Simplified memory extraction - would use NLP in production
        if (userMessage.toLowerCase().contains("my name is")) {
            return "User's name: " + userMessage.substring(userMessage.toLowerCase().indexOf("my name is") + 11);
        }
        
        if (userMessage.toLowerCase().contains("i like") || userMessage.toLowerCase().contains("i love")) {
            return "User preference: " + userMessage;
        }
        
        if (aiResponse.toLowerCase().contains("i'll remember") || aiResponse.toLowerCase().contains("noted")) {
            return "Important: " + userMessage;
        }
        
        return null;
    }
    
    private String determineMemoryType(String content) {
        content = content.toLowerCase();
        
        if (content.contains("name") || content.contains("age") || content.contains("location")) {
            return "fact";
        } else if (content.contains("like") || content.contains("prefer") || content.contains("favorite")) {
            return "preference";
        } else if (content.contains("happened") || content.contains("did") || content.contains("went")) {
            return "event";
        } else {
            return "general";
        }
    }
    
    private double calculateImportance(String content) {
        // Simple importance calculation - would be more sophisticated in production
        double importance = 0.5;
        
        String[] importantKeywords = {"always", "never", "important", "remember", "love", "hate"};
        for (String keyword : importantKeywords) {
            if (content.toLowerCase().contains(keyword)) {
                importance += 0.1;
            }
        }
        
        return Math.min(importance, 1.0);
    }
    
    private void updateCharacterContextMemory(UUID userId, UUID characterId, String memory) {
        characterContextRepository.findByUserIdAndCharacterId(userId, characterId)
                .ifPresent(context -> {
                    if (context.getSharedMemories().size() >= 100) {
                        // Remove oldest memories if limit reached
                        context.getSharedMemories().remove(0);
                    }
                    
                    context.getSharedMemories().add(memory);
                    context.setLastUpdated(LocalDateTime.now());
                    characterContextRepository.save(context);
                });
    }
}
