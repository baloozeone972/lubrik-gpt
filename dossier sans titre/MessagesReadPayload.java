package com.virtualcompanion.conversationservice.dto;

public class MessagesReadPayload {
    private String userId;
    private List<String> messageIds;
    private LocalDateTime readAt;
}
