package com.virtualcompanion.moderationservice.entity;

public class UserModerationHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "action_type", nullable = false)
    private String actionType; // WARNING, CONTENT_REMOVED, ACCOUNT_SUSPENDED
    
    @Column(n = "reason", nullable = false)
    private String reason;
    
    @Column(n = "severity")
    private Integer severity;
    
    @Column(n = "content_reference")
    private String contentReference;
    
    @Column(n = "duration_hours")
    private Integer durationHours; // Pour les suspensions
    
    @Column(n = "moderator_id")
    private UUID moderatorId;
    
    @Column(n = "auto_moderated")
    private boolean autoModerated = true;
    
    @Column(n = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
