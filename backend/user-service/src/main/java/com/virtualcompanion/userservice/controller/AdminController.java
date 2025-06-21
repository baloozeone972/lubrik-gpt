package com.virtualcompanion.userservice.controller;

public class AdminController {
    
    private final UserService userService;
    private final AdminService adminService;
    
    @PostMapping("/{userId}/lock")
    @Operation(summary = "Lock user account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lockUserAccount(
            @PathVariable UUID userId,
            @Valid @RequestBody LockAccountRequest request) {
        userService.lockUserAccount(userId, request.getReason());
    }
    
    @PostMapping("/{userId}/unlock")
    @Operation(summary = "Unlock user account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlockUserAccount(@PathVariable UUID userId) {
        userService.unlockUserAccount(userId);
    }
    
    @PutMapping("/{userId}/subscription")
    @Operation(summary = "Update user subscription")
    public ResponseEntity<Void> updateUserSubscription(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        userService.updateSubscription(userId, request);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Get user statistics")
    public ResponseEntity<UserStatisticsSummary> getUserStatistics(
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate) {
        UserStatisticsSummary statistics = adminService.getUserStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }
    
    @GetMapping("/sessions")
    @Operation(summary = "Get active user sessions")
    public ResponseEntity<Page<UserSessionResponse>> getActiveSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<UserSessionResponse> sessions = adminService.getActiveSessions(pageRequest);
        return ResponseEntity.ok(sessions);
    }
    
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Invalidate user session")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invalidateSession(@PathVariable UUID sessionId) {
        adminService.invalidateSession(sessionId);
    }
    
    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<AuditLogResponse> logs = adminService.getAuditLogs(userId, action, startDate, endDate, pageRequest);
        return ResponseEntity.ok(logs);
    }
    
    @PostMapping("/bulk-operations")
    @Operation(summary = "Perform bulk operations on users")
    public ResponseEntity<BulkOperationResponse> performBulkOperation(
            @Valid @RequestBody BulkOperationRequest request) {
        BulkOperationResponse response = adminService.performBulkOperation(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/compliance-report")
    @Operation(summary = "Generate compliance report")
    public ResponseEntity<ComplianceReportResponse> generateComplianceReport(
            @RequestParam(required = false) String jurisdiction) {
        ComplianceReportResponse report = adminService.generateComplianceReport(jurisdiction);
        return ResponseEntity.ok(report);
    }
    
    @PostMapping("/export")
    @Operation(summary = "Export user data")
    public ResponseEntity<ExportResponse> exportUserData(
            @Valid @RequestBody ExportRequest request) {
        ExportResponse response = adminService.exportUserData(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/import")
    @Operation(summary = "Import user data")
    public ResponseEntity<ImportResponse> importUserData(
            @Valid @RequestBody ImportRequest request) {
        ImportResponse response = adminService.importUserData(request);
        return ResponseEntity.ok(response);
    }
}
