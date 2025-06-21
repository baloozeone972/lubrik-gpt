package com.virtualcompanion.moderationservice.controller;

public class ModerationController {
    
    private final ModerationService moderationService;
    
    @PostMapping("/text")
    @Operation(summary = "Moderate text content")
    public ResponseEntity<TextModerationResponse> moderateText(
            @Valid @RequestBody TextModerationRequest request) {
        
        TextModerationResponse response = moderationService.moderateText(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/image")
    @Operation(summary = "Moderate image content")
    public ResponseEntity<ImageModerationResponse> moderateImage(
            @Valid @RequestBody ImageModerationRequest request) {
        
        ImageModerationResponse response = moderationService.moderateImage(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/decisions/{moderationId}")
    @Operation(summary = "Get moderation decision")
    public ResponseEntity<ModerationDecision> getModerationDecision(
            @PathVariable UUID moderationId) {
        
        ModerationDecision decision = moderationService.getModerationDecision(moderationId);
        return ResponseEntity.ok(decision);
    }
    
    @PostMapping("/decisions/{moderationId}/review")
    @Operation(summary = "Review moderation decision")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> reviewModeration(
            @PathVariable UUID moderationId,
            @Valid @RequestBody ModerationDecision decision) {
        
        moderationService.reviewModeration(moderationId, decision);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/reports")
    @Operation(summary = "Report content")
    public ResponseEntity<ContentReportResponse> reportContent(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ContentReportRequest request) {
        
        request.setReporterId(userId);
        ContentReportResponse response = moderationService.reportContent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/appeals")
    @Operation(summary = "Submit appeal")
    public ResponseEntity<AppealResponse> submitAppeal(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AppealRequest request) {
        
        request.setUserId(userId);
        AppealResponse response = moderationService.submitAppeal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/age-verification")
    @Operation(summary = "Verify user age")
    public ResponseEntity<AgeVerificationResponse> verifyAge(
            @Valid @RequestBody AgeVerificationRequest request) {
        
        AgeVerificationResponse response = moderationService.verifyAge(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Get moderation statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModerationStatistics> getStatistics(
            @RequestParam(defaultValue = "monthly") String period) {
        
        ModerationStatistics statistics = moderationService.getModerationStatistics(period);
        return ResponseEntity.ok(statistics);
    }
}
