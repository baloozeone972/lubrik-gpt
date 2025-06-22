package com.virtualcompanion.moderationservice.entity;

public class ModerationRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "content_id")
    private String contentId;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "content_type", nullable = false)
    private ContentType contentType;
    
    @Column(n = "content_text", columnDefinition = "TEXT")
    private String contentText;
    
    @Column(n = "content_url")
    private String contentUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationStatus status = ModerationStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "priority")
    private Priority priority = Priority.NORMAL;
    
    @OneToMany(mappedBy = "moderationRequest", cascade = CascadeType.ALL)
    private List<ModerationResult> results = new ArrayList<>();
    
    @Column(n = "final_decision")
    private String finalDecision;
    
    @Column(n = "final_score", precision = 3, scale = 2)
    private Double finalScore;
    
    @ElementCollection
    @CollectionTable(n = "moderation_labels", 
        joinColumns = @JoinColumn(n = "moderation_request_id"))
    @Column(n = "label")
    private Set<String> detectedLabels = new HashSet<>();
    
    @Column(n = "requires_human_review")
    private boolean requiresHumanReview = false;
    
    @Column(n = "reviewed_by")
    private UUID reviewedBy;
    
    @Column(n = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
    
    @Column(n = "auto_action_taken")
    private String autoActionTaken;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(n = "completed_at")
    private LocalDateTime completedAt;
    
    public enum ContentType {
        TEXT_MESSAGE,
        IMAGE,
        VIDEO,
        AUDIO,
        CHARACTER_PROFILE,
        USER_PROFILE,
        VOICE_SAMPLE
    }
    
    public enum ModerationStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REQUIRES_REVIEW
    }
    
    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
}
