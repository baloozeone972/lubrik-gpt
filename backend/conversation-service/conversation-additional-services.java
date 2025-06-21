// AIProcessorService.java
package com.virtualcompanion.conversationservice.service;

import com.virtualcompanion.conversationservice.document.CharacterContext;
import com.virtualcompanion.conversationservice.document.ConversationContext;
import com.virtualcompanion.conversationservice.document.Message;
import com.virtualcompanion.conversationservice.dto.AIResponse;
import com.virtualcompanion.conversationservice.dto.MessageOptions;
import com.virtualcompanion.conversationservice.dto.StreamChunk;
import com.virtualcompanion.conversationservice.entity.Conversation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AIProcessorService {
    Mono<AIResponse> generateResponse(Conversation conversation,
                                     Message userMessage,
                                     List<Message> conversationHistory,
                                     ConversationContext conversationContext,
                                     CharacterContext characterContext,
                                     MessageOptions options);
    
    Flux<StreamChunk> streamResponse(Conversation conversation,
                                    Message userMessage,
                                    MessageOptions options);
}

// AIProcessorServiceImpl.java
package com.virtualcompanion.conversationservice.service.impl;

import com.virtualcompanion.conversationservice.client.CharacterServiceClient;
import com.virtualcompanion.conversationservice.document.CharacterContext;
import com.virtualcompanion.conversationservice.document.ConversationContext;
import com.virtualcompanion.conversationservice.document.Message;
import com.virtualcompanion.conversationservice.dto.*;
import com.virtualcompanion.conversationservice.entity.Conversation;
import com.virtualcompanion.conversationservice.service.AIProcessorService;
import com.virtualcompanion.conversationservice.service.EmotionAnalysisService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIProcessorServiceImpl implements AIProcessorService {
    
    private final CharacterServiceClient characterClient;
    private final EmotionAnalysisService emotionService;
    
    @Value("${ai.model.name:gpt-4}")
    private String modelName;
    
    @Value("${ai.model.temperature:0.7}")
    private Double temperature;
    
    @Value("${ai.model.max-tokens:500}")
    private Integer maxTokens;
    
    @Value("${openai.api.key}")
    private String openAiApiKey;
    
    @Override
    public Mono<AIResponse> generateResponse(Conversation conversation,
                                            Message userMessage,
                                            List<Message> conversationHistory,
                                            ConversationContext conversationContext,
                                            CharacterContext characterContext,
                                            MessageOptions options) {
        
        return Mono.fromCallable(() -> {
            // Get character details
            CharacterDetails character = characterClient.getCharacter(conversation.getCharacterId())
                    .orElseThrow(() -> new RuntimeException("Character not found"));
            
            // Build prompt
            String systemPrompt = buildSystemPrompt(character, conversationContext, characterContext);
            List<ChatMessage> messages = buildChatMessages(systemPrompt, conversationHistory, userMessage);
            
            // Configure model
            StreamingChatLanguageModel model = OpenAiStreamingChatModel.builder()
                    .apiKey(openAiApiKey)
                    .modelName(modelName)
                    .temperature(conversationContext.getSettings() != null && 
                               conversationContext.getSettings().getTemperature() != null ?
                               conversationContext.getSettings().getTemperature() : temperature)
                    .maxTokens(options != null && options.getMaxResponseLength() != null ?
                              options.getMaxResponseLength() : maxTokens)
                    .build();
            
            // Generate response
            StringBuilder responseBuilder = new StringBuilder();
            model.generate(messages, response -> {
                if (response != null) {
                    responseBuilder.append(response);
                }
            });
            
            String responseContent = responseBuilder.toString();
            
            // Analyze emotion if enabled
            EmotionData emotion = null;
            if (options != null && Boolean.TRUE.equals(options.getIncludeEmotions())) {
                emotion = emotionService.analyzeEmotion(responseContent, character.getPersonality());
            }
            
            // Extract action if enabled
            ActionData action = null;
            if (options != null && Boolean.TRUE.equals(options.getIncludeActions())) {
                action = extractAction(responseContent);
            }
            
            // Determine if response is significant for memory
            boolean isSignificant = analyzeSignificance(userMessage.getContent(), responseContent);
            
            return AIResponse.builder()
                    .content(responseContent)
                    .emotion(emotion)
                    .action(action)
                    .isSignificant(isSignificant)
                    .metadata(null)
                    .build();
        });
    }
    
    @Override
    public Flux<StreamChunk> streamResponse(Conversation conversation,
                                           Message userMessage,
                                           MessageOptions options) {
        
        return Flux.create(sink -> {
            try {
                // Get character details
                CharacterDetails character = characterClient.getCharacter(conversation.getCharacterId())
                        .orElseThrow(() -> new RuntimeException("Character not found"));
                
                // Build prompt
                String systemPrompt = buildSystemPrompt(character, null, null);
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from(systemPrompt));
                messages.add(UserMessage.from(userMessage.getContent()));
                
                // Configure streaming model
                StreamingChatLanguageModel model = OpenAiStreamingChatModel.builder()
                        .apiKey(openAiApiKey)
                        .modelName(modelName)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .build();
                
                AtomicInteger chunkIndex = new AtomicInteger(0);
                StringBuilder fullResponse = new StringBuilder();
                
                // Stream response
                model.generate(messages, chunk -> {
                    if (chunk != null && !chunk.isEmpty()) {
                        fullResponse.append(chunk);
                        
                        StreamChunk streamChunk = StreamChunk.builder()
                                .content(chunk)
                                .index(chunkIndex.getAndIncrement())
                                .isComplete(false)
                                .build();
                        
                        sink.next(streamChunk);
                    }
                });
                
                // Send completion chunk
                StreamChunk completionChunk = StreamChunk.builder()
                        .content("")
                        .index(chunkIndex.get())
                        .isComplete(true)
                        .fullContent(fullResponse.toString())
                        .build();
                
                sink.next(completionChunk);
                sink.complete();
                
            } catch (Exception e) {
                log.error("Error in AI streaming: {}", e.getMessage());
                sink.error(e);
            }
        });
    }
    
    private String buildSystemPrompt(CharacterDetails character,
                                   ConversationContext conversationContext,
                                   CharacterContext characterContext) {
        
        StringBuilder prompt = new StringBuilder();
        
        // Character identity
        prompt.append("You are ").append(character.getName());
        if (character.getDescription() != null) {
            prompt.append(", ").append(character.getDescription());
        }
        prompt.append(".\n\n");
        
        // Personality traits
        if (character.getPersonality() != null) {
            prompt.append("Personality traits:\n");
            prompt.append("- Openness: ").append(character.getPersonality().getOpenness()).append("\n");
            prompt.append("- Conscientiousness: ").append(character.getPersonality().getConscientiousness()).append("\n");
            prompt.append("- Extraversion: ").append(character.getPersonality().getExtraversion()).append("\n");
            prompt.append("- Agreeableness: ").append(character.getPersonality().getAgreeableness()).append("\n");
            prompt.append("- Neuroticism: ").append(character.getPersonality().getNeuroticism()).append("\n\n");
        }
        
        // Backstory
        if (character.getBackstory() != null) {
            prompt.append("Backstory: ").append(character.getBackstory()).append("\n\n");
        }
        
        // Conversation settings
        if (conversationContext != null && conversationContext.getSettings() != null) {
            ConversationSettings settings = conversationContext.getSettings();
            if (settings.getResponseStyle() != null) {
                prompt.append("Respond in a ").append(settings.getResponseStyle()).append(" style.\n");
            }
        }
        
        // Character context (relationship history)
        if (characterContext != null) {
            if (characterContext.getRelationshipLevel() > 0) {
                prompt.append("You have an established relationship with the user (level ")
                      .append(characterContext.getRelationshipLevel()).append(").\n");
            }
            
            if (!characterContext.getSharedMemories().isEmpty()) {
                prompt.append("Shared memories:\n");
                characterContext.getSharedMemories().stream()
                        .limit(5)
                        .forEach(memory -> prompt.append("- ").append(memory).append("\n"));
                prompt.append("\n");
            }
        }
        
        // General instructions
        prompt.append("Stay in character and respond naturally. ");
        prompt.append("Be engaging and maintain the conversation flow. ");
        prompt.append("Remember previous context when relevant.");
        
        return prompt.toString();
    }
    
    private List<ChatMessage> buildChatMessages(String systemPrompt,
                                               List<Message> conversationHistory,
                                               Message currentMessage) {
        
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        
        // Add conversation history
        for (Message msg : conversationHistory) {
            if ("user".equals(msg.getRole())) {
                messages.add(UserMessage.from(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(AiMessage.from(msg.getContent()));
            }
        }
        
        // Add current message
        messages.add(UserMessage.from(currentMessage.getContent()));
        
        return messages;
    }
    
    private ActionData extractAction(String response) {
        // Simple action extraction - in production would use more sophisticated NLP
        if (response.contains("*") && response.lastIndexOf("*") > response.indexOf("*")) {
            String actionText = response.substring(
                    response.indexOf("*") + 1,
                    response.lastIndexOf("*")
            );
            
            return ActionData.builder()
                    .type("gesture")
                    .description(actionText)
                    .duration(3.0)
                    .build();
        }
        
        return null;
    }
    
    private boolean analyzeSignificance(String userMessage, String aiResponse) {
        // Simple significance analysis - would be more sophisticated in production
        String[] significantKeywords = {
                "love", "hate", "important", "remember", "never forget",
                "always", "promise", "secret", "confession", "truth"
        };
        
        String combined = (userMessage + " " + aiResponse).toLowerCase();
        
        for (String keyword : significantKeywords) {
            if (combined.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
}

// MemoryService.java
package com.virtualcompanion.conversationservice.service;

import com.virtualcompanion.conversationservice.document.Message;
import com.virtualcompanion.conversationservice.dto.MemoryItem;
import com.virtualcompanion.conversationservice.entity.Conversation;

import java.util.List;
import java.util.UUID;

public interface MemoryService {
    void extractAndStoreMemory(Conversation conversation, Message userMessage, Message aiMessage);
    void consolidateConversationMemory(UUID userId, UUID conversationId);
    void updateCharacterMemory(UUID userId, UUID characterId, List<MemoryItem> memories);
    List<String> retrieveRelevantMemories(UUID userId, UUID characterId, String query, int limit);
}

// MemoryServiceImpl.java
package com.virtualcompanion.conversationservice.service.impl;

import com.virtualcompanion.conversationservice.document.CharacterContext;
import com.virtualcompanion.conversationservice.document.Message;
import com.virtualcompanion.conversationservice.dto.MemoryItem;
import com.virtualcompanion.conversationservice.entity.Conversation;
import com.virtualcompanion.conversationservice.entity.ConversationMemory;
import com.virtualcompanion.conversationservice.repository.CharacterContextRepository;
import com.virtualcompanion.conversationservice.repository.ConversationMemoryRepository;
import com.virtualcompanion.conversationservice.repository.MessageRepository;
import com.virtualcompanion.conversationservice.service.EmbeddingService;
import com.virtualcompanion.conversationservice.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
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