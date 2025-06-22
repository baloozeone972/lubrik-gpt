package com.virtualcompanion.moderationservice.entity;

public class ModerationResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "moderation_request_id", nullable = false)
    private ModerationRequest moderationRequest;
    
    @Column(n = "provider", nullable = false)
    private String provider; // openai, google, aws, local
    
    @Column(n = "model_version")
    private String modelVersion;
    
    @Column(n = "is_safe")
    private boolean isSafe;
    
    @Column(n = "confidence_score", precision = 3, scale = 2)
    private Double confidenceScore;
    
    @Column(n = "categories", columnDefinition = "JSON")
    private String categories; // JSON des catégories détectées
    
    @Column(n = "detailed_scores", columnDefinition = "JSON")
    private String detailedScores; // Scores par catégorie
    
    @Column(n = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(n = "error_message")
    private String errorMessage;
    
    @Column(n = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
