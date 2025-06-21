package com.virtualcompanion.conversationservice;

public class StreamingSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "conversation_id", nullable = false)
    private UUID conversationId;
    
    @Column(n = "user_id", nullable = false)
    private UUID userId;
    
    @Column(n = "session_token", unique = true, nullable = false)
    private String sessionToken;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StreamingStatus status = StreamingStatus.CONNECTING;
    
    @Column(n = "connection_type")
    private String connectionType; // websocket, sse
    
    @Column(n = "client_ip")
    private String clientIp;
    
    @Column(n = "user_agent")
    private String userAgent;
    
    @Column(n = "connected_at")
    @CreationTimestamp
    private LocalDateTime connectedAt;
    
    @Column(n = "disconnected_at")
    private LocalDateTime disconnectedAt;
    
    @Column(n = "last_activity_at")
    private LocalDateTime lastActivityAt;
    
    @Column(n = "messages_sent")
    private Integer messagesSent = 0;
    
    @Column(n = "messages_received")
    private Integer messagesReceived = 0;
    
    public enum StreamingStatus {
        CONNECTING,
        CONNECTED,
        ACTIVE,
        IDLE,
        DISCONNECTED,
        ERROR
    }
}
