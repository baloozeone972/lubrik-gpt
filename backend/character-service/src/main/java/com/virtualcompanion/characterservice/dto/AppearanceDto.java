package com.virtualcompanion.characterservice.dto;

public class AppearanceDto {
    
    @Size(max = 20, message = "Height cannot exceed 20 characters")
    private String height;
    
    @Size(max = 20, message = "Weight cannot exceed 20 characters")
    private String weight;
    
    @Size(max = 50, message = "Hair color cannot exceed 50 characters")
    private String hairColor;
    
    @Size(max = 50, message = "Hair style cannot exceed 50 characters")
    private String hairStyle;
    
    @Size(max = 50, message = "Eye color cannot exceed 50 characters")
    private String eyeColor;
    
    @Size(max = 50, message = "Skin tone cannot exceed 50 characters")
    private String skinTone;
    
    @Size(max = 100, message = "Body type cannot exceed 100 characters")
    private String bodyType;
    
    @Size(max = 100, message = "Ethnicity cannot exceed 100 characters")
    private String ethnicity;
    
    @Size(max = 500, message = "Clothing style cannot exceed 500 characters")
    private String clothingStyle;
    
    @Size(max = 500, message = "Distinguishing features cannot exceed 500 characters")
    private String distinguishingFeatures;
}
