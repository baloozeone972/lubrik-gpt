package com.virtualcompanion.billingservice.controller;

public class AdminBillingController {
    
    private final BillingReportService reportService;
    
    @PostMapping("/reports/revenue")
    @Operation(summary = "Generate revenue report")
    public ResponseEntity<BillingReportResponse> generateRevenueReport(
            @Valid @RequestBody BillingReportRequest request) {
        
        BillingReportResponse report = reportService.generateRevenueReport(request);
        return ResponseEntity.ok(report);
    }
    
    @PostMapping("/reports/subscriptions")
    @Operation(summary = "Generate subscription report")
    public ResponseEntity<BillingReportResponse> generateSubscriptionReport(
            @Valid @RequestBody BillingReportRequest request) {
        
        BillingReportResponse report = reportService.generateSubscriptionReport(request);
        return ResponseEntity.ok(report);
    }
    
    @PostMapping("/reports/usage")
    @Operation(summary = "Generate usage report")
    public ResponseEntity<BillingReportResponse> generateUsageReport(
            @Valid @RequestBody BillingReportRequest request) {
        
        BillingReportResponse report = reportService.generateUsageReport(request);
        return ResponseEntity.ok(report);
    }
    
    @PostMapping("/jobs/process-renewals")
    @Operation(summary = "Manually trigger subscription renewals")
    public ResponseEntity<Void> triggerRenewalProcessing() {
        reportService.triggerRenewalProcessing();
        return ResponseEntity.accepted().build();
    }
    
    @PostMapping("/jobs/process-trials")
    @Operation(summary = "Manually trigger trial conversions")
    public ResponseEntity<Void> triggerTrialProcessing() {
        reportService.triggerTrialProcessing();
        return ResponseEntity.accepted().build();
    }
}
