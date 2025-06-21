package com.virtualcompanion.conversationservice.dto;

public class StreamingMessageEvent {
    
    private String eventType; // message_start, message_chunk, message_end, typing, error
    private String conversationId;
    private String messageId;
    private String content;
    private Integer chunkIndex;
    private Boolean isComplete;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}
