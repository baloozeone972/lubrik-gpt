package com.virtualcompanion.moderationservice.entity;

public class BlockedContent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "content_hash", unique = true)
    private String contentHash;
    
    @Column(n = "content_type")
    private String contentType;
    
    @Column(n = "blocked_reason", nullable = false)
    private String blockedReason;
    
    @Column(n = "severity_level")
    private Integer severityLevel;
    
    @Column(n = "categories", columnDefinition = "JSON")
    private String categories;
    
    @Column(n = "action_taken")
    private String actionTaken;
    
    @Column(n = "appeal_status")
    private String appealStatus;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(n = "expires_at")
    private LocalDateTime expiresAt;
}
