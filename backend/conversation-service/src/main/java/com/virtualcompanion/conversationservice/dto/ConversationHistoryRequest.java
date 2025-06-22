package com.virtualcompanion.conversationservice.dto;

public class ConversationHistoryRequest {

    private UUID characterId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer limit;
    private String sortOrder; // asc, desc
}
