package com.virtualcompanion.billingservice.controller;

public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping
    @Operation(summary = "Process a payment")
    public ResponseEntity<PaymentResponse> processPayment(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PaymentRequest request) {
        
        PaymentResponse payment = paymentService.processPayment(userId, request);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details")
    public ResponseEntity<PaymentResponse> getPayment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID paymentId) {
        
        PaymentResponse payment = paymentService.getPayment(userId, paymentId);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping
    @Operation(summary = "Get user's payment history")
    public ResponseEntity<Page<PaymentResponse>> getPaymentHistory(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentResponse> payments = paymentService.getUserPayments(userId, pageRequest);
        return ResponseEntity.ok(payments);
    }
    
    @PostMapping("/refund")
    @Operation(summary = "Request a refund")
    public ResponseEntity<RefundResponse> requestRefund(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RefundRequest request) {
        
        RefundResponse refund = paymentService.processRefund(userId, request);
        return ResponseEntity.ok(refund);
    }
    
    @GetMapping("/methods")
    @Operation(summary = "Get saved payment methods")
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods(
            @AuthenticationPrincipal UUID userId) {
        
        List<PaymentMethodResponse> methods = paymentService.getUserPaymentMethods(userId);
        return ResponseEntity.ok(methods);
    }
    
    @PostMapping("/methods")
    @Operation(summary = "Add a payment method")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PaymentMethodResponse> addPaymentMethod(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreatePaymentMethodRequest request) {
        
        PaymentMethodResponse method = paymentService.addPaymentMethod(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(method);
    }
    
    @DeleteMapping("/methods/{methodId}")
    @Operation(summary = "Remove a payment method")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePaymentMethod(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID methodId) {
        
        paymentService.removePaymentMethod(userId, methodId);
    }
    
    @PostMapping("/methods/{methodId}/set-default")
    @Operation(summary = "Set default payment method")
    public ResponseEntity<Void> setDefaultPaymentMethod(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID methodId) {
        
        paymentService.setDefaultPaymentMethod(userId, methodId);
        return ResponseEntity.ok().build();
    }
}
