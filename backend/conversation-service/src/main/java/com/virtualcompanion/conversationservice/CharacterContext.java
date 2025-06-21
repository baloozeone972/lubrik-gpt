package com.virtualcompanion.conversationservice;

public class CharacterContext {
    private UUID characterId;
    private String characterN;
    private Map<String, Object> personality;
    private String currentMood;
    private Map<String, Double> moodHistory;
    private String preferredResponseStyle;
    private Map<String, Object> customSettings;
}
