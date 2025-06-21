package com.virtualcompanion.moderationservice.service.impl;

class AggregatedModerationResult {
        private double confidenceScore;
        private boolean hasViolations;
        private String primaryViolation;
        private String reason;
        private List<String> violatedPolicies;
        private List<ViolationDetail> violations;
        private Map<String, Double> categoryScores;
        private String moderatedText;
        private List<String> flaggedPhrases;
    }
