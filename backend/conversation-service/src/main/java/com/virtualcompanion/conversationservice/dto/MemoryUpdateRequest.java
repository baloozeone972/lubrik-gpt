package com.virtualcompanion.conversationservice.dto;

public class MemoryUpdateRequest {

    @NotNull
    private UUID characterId;

    private List<MemoryItem> memories;

    private Map<String, Object> context;
}
