package com.virtualcompanion.characterservice.dto;

public class PersonalityTraitsDto {

    @NotNull
    @DecimalMin(value = "0.0", message = "Openness must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Openness cannot exceed 1.0")
    private Double openness;

    @NotNull
    @DecimalMin(value = "0.0", message = "Conscientiousness must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Conscientiousness cannot exceed 1.0")
    private Double conscientiousness;

    @NotNull
    @DecimalMin(value = "0.0", message = "Extraversion must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Extraversion cannot exceed 1.0")
    private Double extraversion;

    @NotNull
    @DecimalMin(value = "0.0", message = "Agreeableness must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Agreeableness cannot exceed 1.0")
    private Double agreeableness;

    @NotNull
    @DecimalMin(value = "0.0", message = "Neuroticism must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Neuroticism cannot exceed 1.0")
    private Double neuroticism;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String dominantTrait;

    @Size(max = 1000, message = "Behavior notes cannot exceed 1000 characters")
    private String behaviorNotes;
}
