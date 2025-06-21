package com.virtualcompanion.conversationservice.dto;

public class SendMessageRequest {
    
    @NotBlank(message = "Message content is required")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    private String content;
    
    private String messageType; // text, voice, image
    
    private Map<String, Object> metadata;
    
    private MessageOptions options;
}
