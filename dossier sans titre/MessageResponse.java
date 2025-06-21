package com.virtualcompanion.conversationservice.dto;

public class MessageResponse {
    
    private String id;
    private String role; // user, assistant
    private String content;
    private String messageType;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    private EmotionData emotion;
    private ActionData action;
    private String voiceUrl;
    private Long processingTime;
}
