package com.virtualcompanion.moderationservice.controller;

public class AdminModerationController {
    
    private final AdminModerationService adminService;
    
    @GetMapping("/queue")
    @Operation(summary = "Get moderation queue")
    public ResponseEntity<Page<ModerationQueueItem>> getModerationQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ModerationQueueItem> queue = adminService.getModerationQueue(status, priority, pageRequest);
        return ResponseEntity.ok(queue);
    }
    
    @GetMapping("/reports")
    @Operation(summary = "Get content reports")
    public ResponseEntity<Page<ContentReportDetail>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<ContentReportDetail> reports = adminService.getReports(status, pageRequest);
        return ResponseEntity.ok(reports);
    }
    
    @PostMapping("/reports/{reportId}/resolve")
    @Operation(summary = "Resolve content report")
    public ResponseEntity<Void> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReportRequest request) {
        
        adminService.resolveReport(reportId, request);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/appeals")
    @Operation(summary = "Get appeals")
    public ResponseEntity<Page<AppealDetail>> getAppeals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "submittedAt"));
        Page<AppealDetail> appeals = adminService.getAppeals(status, pageRequest);
        return ResponseEntity.ok(appeals);
    }
    
    @PostMapping("/appeals/{appealId}/review")
    @Operation(summary = "Review appeal")
    public ResponseEntity<Void> reviewAppeal(
            @PathVariable UUID appealId,
            @Valid @RequestBody ReviewAppealRequest request) {
        
        adminService.reviewAppeal(appealId, request);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/users/{userId}/history")
    @Operation(summary = "Get user moderation history")
    public ResponseEntity<Page<UserModerationHistoryResponse>> getUserHistory(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserModerationHistoryResponse> history = adminService.getUserModerationHistory(userId, pageRequest);
        return ResponseEntity.ok(history);
    }
    
    @PostMapping("/users/{userId}/actions")
    @Operation(summary = "Take action on user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> takeUserAction(
            @PathVariable UUID userId,
            @Valid @RequestBody UserActionRequest request) {
        
        adminService.takeUserAction(userId, request);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/rules")
    @Operation(summary = "Get moderation rules")
    public ResponseEntity<List<ModerationRuleResponse>> getRules(
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String jurisdiction) {
        
        List<ModerationRuleResponse> rules = adminService.getModerationRules(contentType, jurisdiction);
        return ResponseEntity.ok(rules);
    }
    
    @PostMapping("/rules")
    @Operation(summary = "Create moderation rule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModerationRuleResponse> createRule(
            @Valid @RequestBody CreateRuleRequest request) {
        
        ModerationRuleResponse rule = adminService.createRule(request);
        return ResponseEntity.ok(rule);
    }
    
    @PutMapping("/rules/{ruleId}")
    @Operation(summary = "Update moderation rule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModerationRuleResponse> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateRuleRequest request) {
        
        ModerationRuleResponse rule = adminService.updateRule(ruleId, request);
        return ResponseEntity.ok(rule);
    }
    
    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "Delete moderation rule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        adminService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/blocked-content")
    @Operation(summary = "Get blocked content")
    public ResponseEntity<Page<BlockedContentResponse>> getBlockedContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "blockedAt"));
        Page<BlockedContentResponse> blocked = adminService.getBlockedContent(pageRequest);
        return ResponseEntity.ok(blocked);
    }
    
    @PostMapping("/blocked-content")
    @Operation(summary = "Block content")
    public ResponseEntity<Void> blockContent(
            @Valid @RequestBody BlockContentRequest request) {
        
        adminService.blockContent(request);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/blocked-content/{blockId}")
    @Operation(summary = "Unblock content")
    public ResponseEntity<Void> unblockContent(@PathVariable UUID blockId) {
        adminService.unblockContent(blockId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/reports/export")
    @Operation(summary = "Export moderation report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExportResponse> exportReport(
            @Valid @RequestBody ExportReportRequest request) {
        
        ExportResponse response = adminService.exportReport(request);
        return ResponseEntity.ok(response);
    }
}
