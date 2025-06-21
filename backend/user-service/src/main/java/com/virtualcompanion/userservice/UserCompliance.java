package com.virtualcompanion.userservice;

public class UserCompliance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "user_id", nullable = false)
    private User user;
    
    @Column(n = "jurisdiction", length = 50)
    private String jurisdiction;
    
    @Column(n = "consent_level", length = 20)
    private String consentLevel;
    
    @Column(n = "terms_accepted_version", length = 20)
    private String termsAcceptedVersion;
    
    @Column(n = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;
    
    @Column(n = "privacy_accepted_version", length = 20)
    private String privacyAcceptedVersion;
    
    @Column(n = "privacy_accepted_at")
    private LocalDateTime privacyAcceptedAt;
    
    @Column(n = "marketing_consent")
    private boolean marketingConsent = false;
    
    @Column(n = "data_processing_consent")
    private boolean dataProcessingConsent = true;
    
    @Column(n = "age_verification_method", length = 50)
    private String ageVerificationMethod;
    
    @Column(n = "age_verified_at")
    private LocalDateTime ageVerifiedAt;
    
    @Column(n = "identity_verification_method", length = 50)
    private String identityVerificationMethod;
    
    @Column(n = "identity_verified_at")
    private LocalDateTime identityVerifiedAt;
    
    @Column(n = "gdpr_data_request_count")
    private int gdprDataRequestCount = 0;
    
    @Column(n = "last_gdpr_request_at")
    private LocalDateTime lastGdprRequestAt;
}
