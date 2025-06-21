package com.virtualcompanion.conversationservice.document;

public class Message {
    
    @Id
    private String id;
    
    @Indexed
    private UUID conversationId;
    
    @Indexed
    private UUID userId;
    
    private UUID characterId;
    
    private MessageType type;
    
    private String content;
    
    private MessageMetadata metadata;
    
    private List<MessageAttachment> attachments;
    
    private MessageAnalysis analysis;
    
    private Integer tokensUsed;
    
    private Long processingTimeMs;
    
    @Indexed
    private LocalDateTime timestamp;
    
    private LocalDateTime editedAt;
    
    private boolean isDeleted;
    
    private String deletedReason;
    
    public enum MessageType {
        USER_MESSAGE,
        CHARACTER_MESSAGE,
        SYSTEM_MESSAGE,
        ERROR_MESSAGE
    }
}
