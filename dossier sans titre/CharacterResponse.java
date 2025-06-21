package com.virtualcompanion.characterservice.dto;

public class CharacterResponse {
    
    private UUID id;
    private String name;
    private String description;
    private String category;
    private String gender;
    private Integer age;
    private String backstory;
    private List<String> tags;
    private String avatarUrl;
    private PersonalityTraitsDto personalityTraits;
    private AppearanceDto appearance;
    private VoiceConfigDto voiceConfig;
    private List<DialogueExampleDto> dialogueExamples;
    private Boolean isPublic;
    private Boolean isNsfw;
    private Boolean isActive;
    private UUID creatorId;
    private String creatorUsername;
    private Long totalConversations;
    private Double averageRating;
    private Long totalRatings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
