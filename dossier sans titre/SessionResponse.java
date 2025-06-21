package com.virtualcompanion.userservice.dto.response;

public class SessionResponse {
    private UUID id;
    private String deviceId;
    private String deviceType;
    private String deviceName;
    private String ipAddress;
    private String locationCountry;
    private String locationCity;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime expiresAt;
    private boolean current;
}
