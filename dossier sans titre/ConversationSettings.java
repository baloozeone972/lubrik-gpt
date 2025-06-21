package com.virtualcompanion.conversationservice.dto;

public class ConversationSettings {
    
    private Boolean saveHistory;
    private String language;
    private Double temperature;
    private Integer maxTokens;
    private String responseStyle; // casual, formal, romantic, friendly
    private Boolean enableEmotions;
    private Boolean enableActions;
}
