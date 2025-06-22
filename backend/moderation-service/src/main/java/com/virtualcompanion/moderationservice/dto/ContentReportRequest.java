package com.virtualcompanion.moderationservice.dto;

public class ContentReportRequest {

    @NotNull(message = "Reporter ID is required")
    private UUID reporterId;

    @NotNull(message = "Content type is required")
    private String contentType; // text, image, video, user, character

    @NotNull(message = "Content ID is required")
    private String contentId;

    @NotNull(message = "Report reason is required")
    private String reason;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private List<String> categories;

    private Map<String, Object> evidence;

    private Boolean isUrgent;
}
