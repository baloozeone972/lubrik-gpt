package com.virtualcompanion.conversationservice.dto;

public class ConversationExportResponse {

    private String exportId;
    private String downloadUrl;
    private Long fileSize;
    private String format;
    private LocalDateTime expiresAt;
    private Integer conversationCount;
}
