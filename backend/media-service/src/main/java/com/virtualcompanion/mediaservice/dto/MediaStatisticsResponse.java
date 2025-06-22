package com.virtualcompanion.mediaservice.dto;

public class MediaStatisticsResponse {

    private Long totalFiles;
    private Long totalSize;
    private Map<String, Long> filesByType;
    private Map<String, Long> sizeByType;
    private Long totalDuration;
    private Long totalTranscodes;
    private Long totalVoiceGenerations;
    private Double totalVoiceCost;
    private Map<String, Long> voiceGenerationsByProvider;
}
