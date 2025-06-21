package com.virtualcompanion.conversationservice;

public class MessageMetadata {
    private String emotion;
    private Double sentimentScore;
    private String intentDetected;
    private Map<String, Object> contextVariables;
    private String voiceId;
    private String animationId;
    private Boolean isFilteredContent;
    private String filterReason;
}
