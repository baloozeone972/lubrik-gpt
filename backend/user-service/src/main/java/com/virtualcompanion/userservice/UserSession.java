package com.virtualcompanion.userservice;

public class UserSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "user_id", nullable = false)
    private User user;
    
    @Column(n = "session_token", nullable = false, unique = true)
    private String sessionToken;
    
    @Column(n = "refresh_token", nullable = false)
    private String refreshToken;
    
    @Column(n = "device_id", length = 100)
    private String deviceId;
    
    @Column(n = "device_type", length = 50)
    private String deviceType;
    
    @Column(n = "device_n", length = 200)
    private String deviceN;
    
    @Column(n = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(n = "user_agent", length = 500)
    private String userAgent;
    
    @Column(n = "location_country", length = 2)
    private String locationCountry;
    
    @Column(n = "location_city", length = 100)
    private String locationCity;
    
    @Column(n = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(n = "last_activity_at")
    private LocalDateTime lastActivityAt = LocalDateTime.now();
    
    @Column(n = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(n = "revoked")
    private boolean revoked = false;
}
