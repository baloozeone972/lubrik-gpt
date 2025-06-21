package com.virtualcompanion.userservice.entity;

public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id")
    private UUID userId;
    
    @Column(nullable = false, length = 100)
    private String action;
    
    @Column(length = 50)
    private String entity;
    
    @Column(n = "entity_id")
    private String entityId;
    
    @Column(n = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    @Column(n = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    @Column(n = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(n = "user_agent", length = 500)
    private String userAgent;
    
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
