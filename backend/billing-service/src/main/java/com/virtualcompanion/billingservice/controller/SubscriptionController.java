package com.virtualcompanion.billingservice.controller;

public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    private final UsageService usageService;
    
    @PostMapping
    @Operation(summary = "Create a new subscription")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        
        SubscriptionResponse subscription = subscriptionService.createSubscription(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }
    
    @GetMapping("/current")
    @Operation(summary = "Get current subscription")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription(
            @AuthenticationPrincipal UUID userId) {
        
        SubscriptionResponse subscription = subscriptionService.getSubscription(userId);
        return ResponseEntity.ok(subscription);
    }
    
    @PutMapping("/current")
    @Operation(summary = "Update current subscription")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        
        SubscriptionResponse subscription = subscriptionService.updateSubscription(userId, request);
        return ResponseEntity.ok(subscription);
    }
    
    @PostMapping("/current/cancel")
    @Operation(summary = "Cancel subscription")
    public ResponseEntity<Void> cancelSubscription(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "false") boolean immediately) {
        
        subscriptionService.cancelSubscription(userId, immediately);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/current/reactivate")
    @Operation(summary = "Reactivate canceled subscription")
    public ResponseEntity<Void> reactivateSubscription(
            @AuthenticationPrincipal UUID userId) {
        
        subscriptionService.reactivateSubscription(userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/history")
    @Operation(summary = "Get subscription history")
    public ResponseEntity<Page<SubscriptionResponse>> getSubscriptionHistory(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SubscriptionResponse> history = subscriptionService.getUserSubscriptionHistory(userId, pageRequest);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/limits")
    @Operation(summary = "Get subscription limits")
    public ResponseEntity<SubscriptionLimits> getSubscriptionLimits(
            @AuthenticationPrincipal UUID userId) {
        
        SubscriptionLimits limits = subscriptionService.getSubscriptionLimits(userId);
        return ResponseEntity.ok(limits);
    }
    
    @GetMapping("/usage")
    @Operation(summary = "Get current usage statistics")
    public ResponseEntity<UsageStatisticsResponse> getUsageStatistics(
            @AuthenticationPrincipal UUID userId) {
        
        UsageStatisticsResponse usage = usageService.getCurrentPeriodUsage(userId);
        return ResponseEntity.ok(usage);
    }
    
    @PostMapping("/usage")
    @Operation(summary = "Record usage")
    public ResponseEntity<Void> recordUsage(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UsageRecordRequest request) {
        
        usageService.recordUsage(userId, request);
        return ResponseEntity.accepted().build();
    }
}
