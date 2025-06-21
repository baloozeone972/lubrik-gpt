package com.virtualcompanion.moderationservice.dto;

public class DetectionResult {
    
    private String label;
    private Double confidence;
    private String parentLabel;
    private List<String> taxonomyPath;
}
