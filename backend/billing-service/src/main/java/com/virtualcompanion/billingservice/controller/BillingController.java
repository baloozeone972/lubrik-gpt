package com.virtualcompanion.billingservice.controller;

public class BillingController {

    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final UsageTrackingService usageService;
    private final StripeService stripeService;
    private final PayPalService payPalService;

    // ========== Subscription Management ==========

    @GetMapping("/plans")
    @Operation(summary = "Obtenir tous les plans d'abonnement")
    public Flux<SubscriptionPlanDto> getSubscriptionPlans() {
        return subscriptionService.getAllPlans();
    }

    @GetMapping("/subscription")
    @Operation(summary = "Obtenir l'abonnement actuel")
    public Mono<SubscriptionDto> getCurrentSubscription(@CurrentUser UserPrincipal user) {
        return subscriptionService.getCurrentSubscription(user.getId());
    }

    @PostMapping("/subscribe")
    @Operation(summary = "S'abonner à un plan")
    public Mono<SubscriptionResponseDto> subscribe(
            @Valid @RequestBody SubscribeRequestDto request,
            @CurrentUser UserPrincipal user) {
        
        log.info("User {} subscribing to plan: {}", user.getId(), request.getPlanId());
        
        return subscriptionService.createSubscription(user.getId(), request)
                .doOnSuccess(sub -> log.info("Subscription created: {}", sub.getId()));
    }

    @PutMapping("/subscription/upgrade")
    @Operation(summary = "Améliorer l'abonnement")
    public Mono<SubscriptionDto> upgradeSubscription(
            @Valid @RequestBody UpgradeRequestDto request,
            @CurrentUser UserPrincipal user) {
        
        return subscriptionService.upgradeSubscription(user.getId(), request.getNewPlanId());
    }

    @PutMapping("/subscription/downgrade")
    @Operation(summary = "Rétrograder l'abonnement")
    public Mono<SubscriptionDto> downgradeSubscription(
            @Valid @RequestBody DowngradeRequestDto request,
            @CurrentUser UserPrincipal user) {
        
        return subscriptionService.downgradeSubscription(user.getId(), request.getNewPlanId());
    }

    @PostMapping("/subscription/cancel")
    @Operation(summary = "Annuler l'abonnement")
    public Mono<CancellationResponseDto> cancelSubscription(
            @Valid @RequestBody CancelRequestDto request,
            @CurrentUser UserPrincipal user) {
        
        return subscriptionService.cancelSubscription(user.getId(), request);
    }

    @PostMapping("/subscription/reactivate")
    @Operation(summary = "Réactiver l'abonnement")
    public Mono<SubscriptionDto> reactivateSubscription(@CurrentUser UserPrincipal user) {
        return subscriptionService.reactivateSubscription(user.getId());
    }

    // ========== Payment Methods ==========

    @GetMapping("/payment-methods")
    @Operation(summary = "Obtenir les moyens de paiement")
    public Flux<PaymentMethodDto> getPaymentMethods(@CurrentUser UserPrincipal user) {
        return paymentService.getUserPaymentMethods(user.getId());
    }

    @PostMapping("/payment-methods")
    @Operation(summary = "Ajouter un moyen de paiement")
    public Mono<PaymentMethodDto> addPaymentMethod(
            @Valid @RequestBody AddPaymentMethodDto request,
            @CurrentUser UserPrincipal user) {
        
        return paymentService.addPaymentMethod(user.getId(), request);
    }

    @PutMapping("/payment-methods/{methodId}/default")
    @Operation(summary = "Définir comme moyen de paiement par défaut")
    public Mono<Void> setDefaultPaymentMethod(
            @PathVariable String methodId,
            @CurrentUser UserPrincipal user) {
        
        return paymentService.setDefaultPaymentMethod(user.getId(), methodId);
    }

    @DeleteMapping("/payment-methods/{methodId}")
    @Operation(summary = "Supprimer un moyen de paiement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removePaymentMethod(
            @PathVariable String methodId,
            @CurrentUser UserPrincipal user) {
        
        return paymentService.removePaymentMethod(user.getId(), methodId);
    }

    // ========== Payment Processing ==========

    @PostMapping("/checkout/session")
    @Operation(summary = "Créer une session de paiement")
    public Mono<CheckoutSessionDto> createCheckoutSession(
            @Valid @RequestBody CheckoutRequestDto request,
            @CurrentUser UserPrincipal user) {
        
        return switch (request.getProvider()) {
            case STRIPE -> stripeService.createCheckoutSession(user.getId(), request);
            case PAYPAL -> payPalService.createCheckoutSession(user.getId(), request);
            default -> Mono.error(new UnsupportedPaymentProviderException(request.getProvider()));
        };
    }

    @PostMapping("/payment/confirm")
    @Operation(summary = "Confirmer un paiement")
    public Mono<PaymentConfirmationDto> confirmPayment(
            @Valid @RequestBody PaymentConfirmDto request,
            @CurrentUser UserPrincipal user) {
        
        return paymentService.confirmPayment(user.getId(), request);
    }

    @GetMapping("/payments")
    @Operation(summary = "Historique des paiements")
    public Mono<Page<PaymentDto>> getPaymentHistory(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Pageable pageable,
            @CurrentUser UserPrincipal user) {
        
        return paymentService.getUserPaymentHistory(user.getId(), startDate, endDate, pageable);
    }

    // ========== Invoices ==========

    @GetMapping("/invoices")
    @Operation(summary = "Obtenir les factures")
    public Mono<Page<InvoiceDto>> getInvoices(
            Pageable pageable,
            @CurrentUser UserPrincipal user) {
        
        return invoiceService.getUserInvoices(user.getId(), pageable);
    }

    @GetMapping("/invoices/{invoiceId}")
    @Operation(summary = "Obtenir une facture spécifique")
    public Mono<InvoiceDto> getInvoice(
            @PathVariable String invoiceId,
            @CurrentUser UserPrincipal user) {
        
        return invoiceService.getInvoice(invoiceId, user.getId());
    }

    @GetMapping("/invoices/{invoiceId}/download")
    @Operation(summary = "Télécharger une facture PDF")
    public Mono<ResponseEntity<byte[]>> downloadInvoice(
            @PathVariable String invoiceId,
            @CurrentUser UserPrincipal user) {
        
        return invoiceService.generateInvoicePdf(invoiceId, user.getId())
                .map(pdf -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header("Content-Disposition", 
                                "attachment; filename=invoice-" + invoiceId + ".pdf")
                        .body(pdf));
    }

    // ========== Usage Tracking ==========

    @GetMapping("/usage")
    @Operation(summary = "Obtenir l'utilisation actuelle")
    public Mono<UsageSummaryDto> getCurrentUsage(@CurrentUser UserPrincipal user) {
        return usageService.getCurrentMonthUsage(user.getId());
    }

    @GetMapping("/usage/history")
    @Operation(summary = "Historique d'utilisation")
    public Flux<MonthlyUsageDto> getUsageHistory(
            @RequestParam(defaultValue = "6") int months,
            @CurrentUser UserPrincipal user) {
        
        return usageService.getUsageHistory(user.getId(), months);
    }

    @GetMapping("/usage/limits")
    @Operation(summary = "Obtenir les limites d'utilisation")
    public Mono<UsageLimitsDto> getUsageLimits(@CurrentUser UserPrincipal user) {
        return subscriptionService.getUserLimits(user.getId());
    }

    // ========== Webhooks ==========

    @PostMapping("/webhooks/stripe")
    @Operation(summary = "Webhook Stripe", hidden = true)
    public Mono<ResponseEntity<String>> handleStripeWebhook(
            @RequestHeader("Stripe-Signature") String signature,
            @RequestBody String payload) {
        
        return stripeService.handleWebhook(payload, signature)
                .map(event -> ResponseEntity.ok("Webhook processed"))
                .onErrorResume(error -> {
                    log.error("Stripe webhook error: ", error);
                    return Mono.just(ResponseEntity.badRequest().body("Webhook error"));
                });
    }

    @PostMapping("/webhooks/paypal")
    @Operation(summary = "Webhook PayPal", hidden = true)
    public Mono<ResponseEntity<String>> handlePayPalWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody String payload) {
        
        return payPalService.handleWebhook(payload, headers)
                .map(event -> ResponseEntity.ok("Webhook processed"))
                .onErrorResume(error -> {
                    log.error("PayPal webhook error: ", error);
                    return Mono.just(ResponseEntity.badRequest().body("Webhook error"));
                });
    }

    // ========== Admin Endpoints ==========

    @GetMapping("/admin/subscriptions")
    @Operation(summary = "Obtenir toutes les souscriptions (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Page<SubscriptionDto>> getAllSubscriptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String planId,
            Pageable pageable) {
        
        return subscriptionService.getAllSubscriptions(status, planId, pageable);
    }

    @GetMapping("/admin/revenue")
    @Operation(summary = "Obtenir les revenus (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<RevenueReportDto> getRevenueReport(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        
        return subscriptionService.generateRevenueReport(startDate, endDate);
    }

    @PostMapping("/admin/refund")
    @Operation(summary = "Effectuer un remboursement (Admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<RefundResponseDto> processRefund(
            @Valid @RequestBody RefundRequestDto request) {
        
        return paymentService.processRefund(request);
    }
}
