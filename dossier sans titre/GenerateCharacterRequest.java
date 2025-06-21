package com.virtualcompanion.characterservice.dto;

public class GenerateCharacterRequest {
    
    @NotBlank(message = "Prompt is required")
    @Size(max = 1000, message = "Prompt cannot exceed 1000 characters")
    private String prompt;
    
    private String category;
    
    private String gender;
    
    @Size(max = 100, message = "Style cannot exceed 100 characters")
    private String style;
    
    private PersonalityTraitsDto preferredTraits;
}
