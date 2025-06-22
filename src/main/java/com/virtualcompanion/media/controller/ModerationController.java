package com.virtualcompanion.media.controller;

public class ModerationController {

    private final ContentModerationService contentService;
    private final UserModerationService userService;
    private final AutoModerationService autoService;

    @PostMapping("/content/check")
    @Operation(summary = "Check content for violations")
    public Mono<ResponseEntity<ModerationResult>> checkContent(
            @RequestBody @Valid ContentCheckRequest request,
            @AuthenticationPrincipal String userId) {

        return contentService.checkContent(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/report")
    @Operation(summary = "Report content or user")
    public Mono<ResponseEntity<ReportResponse>> reportContent(
            @RequestBody @Valid ReportRequest request,
            @AuthenticationPrincipal String userId) {

        return userService.createReport(request, userId)
                .map(response -> ResponseEntity.status(201).body(response));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Get moderation reports")
    public Mono<ResponseEntity<Page<ReportResponse>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportType type,
            Pageable pageable) {

        return userService.getReports(status, type, pageable)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/reports/{reportId}")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Update report status")
    public Mono<ResponseEntity<ReportResponse>> updateReport(
            @PathVariable UUID reportId,
            @RequestBody @Valid ReportUpdateRequest request,
            @AuthenticationPrincipal String moderatorId) {

        return userService.updateReport(reportId, request, moderatorId)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/users/{userId}/ban")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Ban user")
    public Mono<ResponseEntity<BanResponse>> banUser(
            @PathVariable UUID userId,
            @RequestBody @Valid BanRequest request,
            @AuthenticationPrincipal String moderatorId) {

        return userService.banUser(userId, request, moderatorId)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/users/{userId}/ban")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Unban user")
    public Mono<ResponseEntity<Void>> unbanUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal String moderatorId) {

        return userService.unbanUser(userId, moderatorId)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @GetMapping("/rules")
    @Operation(summary = "Get moderation rules")
    public Mono<ResponseEntity<List<ModerationRule>>> getRules() {
        return autoService.getRules()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create moderation rule")
    public Mono<ResponseEntity<ModerationRule>> createRule(
            @RequestBody @Valid ModerationRuleRequest request) {

        return autoService.createRule(request)
                .map(rule -> ResponseEntity.status(201).body(rule));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Get moderation statistics")
    public Mono<ResponseEntity<ModerationStats>> getStats(
            @RequestParam(required = false) String period) {

        return autoService.getStats(period)
                .map(ResponseEntity::ok);
    }
}
