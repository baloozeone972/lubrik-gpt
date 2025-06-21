package com.virtualcompanion.conversationservice.dto;

public class ActionData {
    
    private String type; // gesture, movement, expression
    private String description;
    private Double duration;
    private Map<String, Object> parameters;
}
