package com.virtualcompanion.characterservice.dto;

public class UpdateCharacterRequest {
    
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    private String category;
    
    @Min(value = 18, message = "Character must be at least 18 years old")
    @Max(value = 100, message = "Character age cannot exceed 100")
    private Integer age;
    
    @Size(max = 1000, message = "Backstory cannot exceed 1000 characters")
    private String backstory;
    
    private List<String> tags;
    
    private PersonalityTraitsDto personalityTraits;
    
    private AppearanceDto appearance;
    
    private VoiceConfigDto voiceConfig;
    
    private List<DialogueExampleDto> dialogueExamples;
    
    private Boolean isPublic;
    
    private Boolean isNsfw;
}
