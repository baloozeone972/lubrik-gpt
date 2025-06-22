package com.virtualcompanion.mediaservice.dto;

public class MediaSearchRequest {

    private UUID userId;
    private UUID characterId;
    private UUID conversationId;
    private List<String> mediaTypes;
    private List<String> contentTypes;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String processingStatus;
    private Boolean isPublic;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer size;
}
