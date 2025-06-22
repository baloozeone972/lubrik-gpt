package com.virtualcompanion.conversationservice.dto;

public class ConversationResponse {

    private UUID id;
    private UUID userId;
    private UUID characterId;
    private String characterName;
    private String status; // active, paused, ended
    private String mode;
    private LocalDateTime startedAt;
    private LocalDateTime lastActivityAt;
    private Long messageCount;
    private ConversationSettings settings;
    private List<MessageResponse> recentMessages;
}
