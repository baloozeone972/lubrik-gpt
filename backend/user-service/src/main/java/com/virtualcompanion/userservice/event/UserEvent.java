package com.virtualcompanion.userservice.event;

public class UserEvent {
    private UUID userId;
    private String eventType;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}
