package com.virtualcompanion.conversationservice.dto;

public class ConversationExportRequest {

    @NotNull
    private List<UUID> conversationIds;

    private String format; // json, txt, pdf

    private Boolean includeMetadata;

    private Boolean includeEmotions;

    private Boolean anonymize;
}
