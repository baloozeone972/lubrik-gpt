package com.virtualcompanion.characterservice.dto;

public class CharacterRatingResponse {
    
    private UUID id;
    private UUID characterId;
    private UUID userId;
    private String username;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
