package com.virtualcompanion.userservice.dto.request;

public class AgeVerificationRequest {
    
    @NotNull(message = "Verification method is required")
    private VerificationMethod method;
    
    private String documentType;
    private MultipartFile documentImage;
    private String documentNumber;
    
    public enum VerificationMethod {
        DOCUMENT,
        CREDIT_CARD,
        BANK_ACCOUNT,
        BIOMETRIC
    }
}
