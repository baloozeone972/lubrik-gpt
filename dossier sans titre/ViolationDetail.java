package com.virtualcompanion.moderationservice.dto;

public class ViolationDetail {
    
    private String category;
    private String severity; // low, medium, high, critical
    private Double confidence;
    private String description;
    private String action; // block, warn, flag, review
}
