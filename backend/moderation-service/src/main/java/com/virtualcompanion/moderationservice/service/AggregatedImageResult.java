package com.virtualcompanion.moderationservice.service;

class AggregatedImageResult {
        private double confidenceScore;
        private boolean hasViolations;
        private String primaryViolation;
        private String reason;
        private String severity;
        private List<String> violatedPolicies;
        private List<ViolationDetail> violations;
        private Map<String, DetectionResult> detections;
        private boolean requiresBlur;
        private List<BoundingBox> sensitiveAreas;
    }
