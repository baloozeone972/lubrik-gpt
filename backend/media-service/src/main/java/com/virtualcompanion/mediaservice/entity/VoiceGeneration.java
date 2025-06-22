package com.virtualcompanion.mediaservice.entity;

public class VoiceGeneration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "character_id", nullable = false)
    private UUID characterId;
    
    @Column(n = "text", columnDefinition = "TEXT", nullable = false)
    private String text;
    
    @Column(n = "voice_id", nullable = false)
    private String voiceId;
    
    @Column(n = "language_code", nullable = false)
    private String languageCode;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationStatus status = GenerationStatus.QUEUED;
    
    // TTS Parameters
    @Column(n = "voice_provider")
    private String voiceProvider; // elevenlabs, azure, google, etc.
    
    @Column(n = "voice_model")
    private String voiceModel;
    
    @Column(n = "speed")
    private Float speed = 1.0f;
    
    @Column(n = "pitch")
    private Float pitch = 0.0f;
    
    @Column(n = "emotion")
    private String emotion;
    
    @Column(n = "emphasis", columnDefinition = "JSON")
    private String emphasis; // word-level emphasis
    
    // Output
    @Column(n = "audio_url")
    private String audioUrl;
    
    @Column(n = "audio_format")
    private String audioFormat = "mp3";
    
    @Column(n = "duration_ms")
    private Integer durationMs;
    
    @Column(n = "file_size")
    private Long fileSize;
    
    // Cost Tracking
    @Column(n = "characters_count")
    private Integer charactersCount;
    
    @Column(n = "credits_used")
    private Integer creditsUsed;
    
    @Column(n = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(n = "error_message")
    private String errorMessage;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(n = "completed_at")
    private LocalDateTime completedAt;
    
    public enum GenerationStatus {
        QUEUED,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
