package com.virtualcompanion.mediaservice.entity;

public class StreamingSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "character_id", nullable = false)
    private UUID characterId;
    
    @Column(n = "conversation_id")
    private UUID conversationId;
    
    @Column(n = "session_id", unique = true, nullable = false)
    private String sessionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StreamType streamType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StreamStatus status = StreamStatus.INITIALIZING;
    
    // WebRTC/Streaming Details
    @Column(n = "offer_sdp", columnDefinition = "TEXT")
    private String offerSdp;
    
    @Column(n = "answer_sdp", columnDefinition = "TEXT")
    private String answerSdp;
    
    @Column(n = "ice_candidates", columnDefinition = "JSON")
    private String iceCandidates;
    
    @Column(n = "media_server_url")
    private String mediaServerUrl;
    
    @Column(n = "media_pipeline_id")
    private String mediaPipelineId;
    
    // Quality Settings
    @Column(n = "video_resolution")
    private String videoResolution = "1280x720";
    
    @Column(n = "video_bitrate")
    private Integer videoBitrate = 2000000;
    
    @Column(n = "audio_bitrate")
    private Integer audioBitrate = 128000;
    
    @Column(n = "frame_rate")
    private Integer frameRate = 30;
    
    // Recording
    @Column(n = "is_recording")
    private boolean isRecording = false;
    
    @Column(n = "recording_path")
    private String recordingPath;
    
    @Column(n = "recording_duration")
    private Integer recordingDuration;
    
    // Analytics
    @Column(n = "packets_sent")
    private Long packetsSent = 0L;
    
    @Column(n = "packets_received")
    private Long packetsReceived = 0L;
    
    @Column(n = "bytes_sent")
    private Long bytesSent = 0L;
    
    @Column(n = "bytes_received")
    private Long bytesReceived = 0L;
    
    @Column(n = "average_latency_ms")
    private Integer averageLatencyMs;
    
    @Column(n = "packet_loss_rate")
    private Float packetLossRate;
    
    @Column(n = "started_at")
    @CreationTimestamp
    private LocalDateTime startedAt;
    
    @Column(n = "ended_at")
    private LocalDateTime endedAt;
    
    public enum StreamType {
        VIDEO_CHAT,
        VOICE_CHAT,
        SCREEN_SHARE,
        BROADCAST
    }
    
    public enum StreamStatus {
        INITIALIZING,
        CONNECTING,
        CONNECTED,
        ACTIVE,
        RECONNECTING,
        ENDED,
        ERROR
    }
}
