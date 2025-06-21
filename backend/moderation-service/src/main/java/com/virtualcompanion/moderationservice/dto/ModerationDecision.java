package com.virtualcompanion.moderationservice.dto;

public class ModerationDecision {
    
    private UUID decisionId;
    private UUID moderationRequestId;
    private String decision; // approve, reject, escalate, warn
    private String decisionType; // automated, human, hybrid
    private UUID moderatorId;
    private String reason;
    private List<String> violatedPolicies;
    private Map<String, Object> actions;
    private LocalDateTime decidedAt;
    private Boolean isAppealable;
}
