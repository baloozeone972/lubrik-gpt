package com.virtualcompanion.mediaservice.entity;

public class MediaFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "file_n", nullable = false)
    private String fileN;
    
    @Column(n = "original_file_n")
    private String originalFileN;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "media_type", nullable = false)
    private MediaType mediaType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaStatus status = MediaStatus.PENDING;
    
    @Column(n = "mime_type", nullable = false)
    private String mimeType;
    
    @Column(n = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(n = "storage_path", nullable = false)
    private String storagePath;
    
    @Column(n = "cdn_url")
    private String cdnUrl;
    
    @Column(n = "thumbnail_url")
    private String thumbnailUrl;
    
    // Metadata
    @Column(n = "width")
    private Integer width;
    
    @Column(n = "height")
    private Integer height;
    
    @Column(n = "duration_seconds")
    private Integer durationSeconds;
    
    @Column(n = "bitrate")
    private Integer bitrate;
    
    @Column(n = "frame_rate")
    private Float frameRate;
    
    @Column(n = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    // Processing
    @OneToMany(mappedBy = "mediaFile", cascade = CascadeType.ALL)
    private List<MediaVariant> variants = new ArrayList<>();
    
    @Column(n = "processing_started_at")
    private LocalDateTime processingStartedAt;
    
    @Column(n = "processing_completed_at")
    private LocalDateTime processingCompletedAt;
    
    @Column(n = "processing_error")
    private String processingError;
    
    // Moderation
    @Column(n = "is_moderated")
    private boolean isModerated = false;
    
    @Column(n = "moderation_score")
    private Double moderationScore;
    
    @Column(n = "moderation_labels", columnDefinition = "JSON")
    private String moderationLabels;
    
    @Column(n = "is_approved")
    private boolean isApproved = true;
    
    // Access Control
    @Column(n = "is_public")
    private boolean isPublic = false;
    
    @Column(n = "access_count")
    private Long accessCount = 0L;
    
    @Column(n = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @Column(n = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum MediaType {
        IMAGE,
        VIDEO,
        AUDIO,
        AVATAR_3D,
        VOICE_SAMPLE,
        DOCUMENT
    }
    
    public enum MediaStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        DELETED
    }
}
