package com.virtualcompanion.conversationservice.dto;

public class MemoryItem {

    private String type; // fact, preference, event, relationship
    private String content;
    private Double importance;
    private String category;
    private Map<String, Object> metadata;
}
