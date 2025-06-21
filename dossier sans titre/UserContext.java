package com.virtualcompanion.conversationservice.document;

public class UserContext {
    private UUID userId;
    private String preferredN;
    private List<String> interests;
    private Map<String, Object> preferences;
    private String communicationStyle;
    private List<String> triggerTopics;
    private Map<String, LocalDateTime> importantDates;
}
