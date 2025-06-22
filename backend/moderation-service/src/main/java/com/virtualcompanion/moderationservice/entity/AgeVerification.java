package com.virtualcompanion.moderationservice.entity;

public class AgeVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false, unique = true)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;
    
    @Column(n = "verification_method")
    private String verificationMethod; // DOCUMENT, CREDIT_CARD, FACIAL_AGE
    
    @Column(n = "document_type")
    private String documentType;
    
    @Column(n = "document_hash")
    private String documentHash; // Hash du document pour éviter les doublons
    
    @Column(n = "estimated_age")
    private Integer estimatedAge;
    
    @Column(n = "birth_date")
    private LocalDate birthDate;
    
    @Column(n = "is_adult")
    private boolean isAdult;
    
    @Column(n = "confidence_score", precision = 3, scale = 2)
    private Double confidenceScore;
    
    @Column(n = "verification_provider")
    private String verificationProvider;
    
    @Column(n = "failure_reason")
    private String failureReason;
    
    @Column(n = "attempts_count")
    private Integer attemptsCount = 0;
    
    @Column(n = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(n = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum VerificationStatus {
        PENDING,
        PROCESSING,
        VERIFIED,
        FAILED,
        EXPIRED
    }
}
