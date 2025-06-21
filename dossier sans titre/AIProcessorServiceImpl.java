package com.virtualcompanion.conversationservice.service.impl;

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
