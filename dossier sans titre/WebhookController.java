package com.virtualcompanion.billingservice.controller;

public class WebhookController {
    
    private final WebhookService webhookService;
    private final SubscriptionService subscriptionService;
    
    @Value("${stripe.webhook-secret}")
    private String stripeWebhookSecret;
    
    @PostMapping("/stripe")
    @Operation(summary = "Handle Stripe webhooks")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        try {
            // Verify webhook signature
            Event event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);
            
            log.info("Received Stripe webhook: {} - {}", event.getType(), event.getId());
            
            // Process webhook
            WebhookPayload webhookPayload = WebhookPayload.builder()
                    .provider("stripe")
                    .eventType(event.getType())
                    .eventId(event.getId())
                    .data(Map.of("event", event))
                    .signature(signature)
                    .build();
            
            webhookService.processWebhook(webhookPayload);
            subscriptionService.handleSubscriptionWebhook("stripe", webhookPayload);
            
            return ResponseEntity.ok("Webhook processed");
            
        } catch (Exception e) {
            log.error("Error processing Stripe webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook processing failed");
        }
    }
    
    @PostMapping("/paypal")
    @Operation(summary = "Handle PayPal webhooks")
    public ResponseEntity<String> handlePayPalWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader Map<String, String> headers) {
        
        try {
            log.info("Received PayPal webhook: {}", payload.get("event_type"));
            
            // Verify webhook (simplified - in production would verify with PayPal)
            WebhookPayload webhookPayload = WebhookPayload.builder()
                    .provider("paypal")
                    .eventType((String) payload.get("event_type"))
                    .eventId((String) payload.get("id"))
                    .data(payload)
                    .build();
            
            webhookService.processWebhook(webhookPayload);
            subscriptionService.handleSubscriptionWebhook("paypal", webhookPayload);
            
            return ResponseEntity.ok("Webhook processed");
            
        } catch (Exception e) {
            log.error("Error processing PayPal webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook processing failed");
        }
    }
}
