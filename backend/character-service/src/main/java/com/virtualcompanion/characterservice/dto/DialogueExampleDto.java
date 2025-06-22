package com.virtualcompanion.characterservice.dto;

public class DialogueExampleDto {

    @NotBlank(message = "Context is required")
    @Size(max = 200, message = "Context cannot exceed 200 characters")
    private String context;

    @NotBlank(message = "User input is required")
    @Size(max = 500, message = "User input cannot exceed 500 characters")
    private String userInput;

    @NotBlank(message = "Character response is required")
    @Size(max = 1000, message = "Character response cannot exceed 1000 characters")
    private String characterResponse;

    @Size(max = 50, message = "Mood cannot exceed 50 characters")
    private String mood;
}
