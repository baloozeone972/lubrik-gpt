package com.virtualcompanion.moderationservice.dto;

public class AgeVerificationRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Verification method is required")
    private String verificationMethod; // self_declaration, document, payment
    
    private LocalDate birthDate;
    
    private String documentType;
    
    private String documentImageUrl;
    
    private String documentNumber;
    
    private String jurisdiction;
    
    private Map<String, Object> additionalData;
}
