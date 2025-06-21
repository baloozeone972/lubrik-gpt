package com.virtualcompanion.conversationservice.dto;

public class StartConversationRequest {
    
    @NotNull(message = "Character ID is required")
    private UUID characterId;
    
    private String initialMessage;
    
    private String conversationMode; // text, voice, video
    
    private Map<String, Object> context;
    
    private ConversationSettings settings;
}
