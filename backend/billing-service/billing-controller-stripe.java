// ========== BillingController.java ==========
package com.virtualcompanion.billingservice.controller;

import com.stripe.model.Event;
import com.virtualcompanion.billingservice.dto.*;
import com.virtualcompanion.billingservice.service.*;
import com.virtualcompanion.common.security.CurrentUser;
import com.virtualcompanion.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Billing", description = "Gestion de la facturation et des abonnements")
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

// ========== StripeService.java ==========
package com.virtualcompanion.billingservice.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import com.virtualcompanion.billingservice.config.StripeConfiguration;
import com.virtualcompanion.billingservice.dto.*;
import com.virtualcompanion.billingservice.entity.Payment;
import com.virtualcompanion.billingservice.entity.Subscription;
import com.virtualcompanion.billingservice.repository.PaymentRepository;
import com.virtualcompanion.billingservice.repository.SubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeService {

    private final StripeConfiguration stripeConfig;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final WebhookEventService webhookService;
    private final NotificationService notificationService;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeConfig.getSecretKey();
        log.info("Stripe service initialized");
    }

    // ========== Customer Management ==========

    public Mono<Customer> createOrGetCustomer(String userId, String email) {
        return Mono.fromCallable(() -> {
            // Chercher le client existant
            CustomerSearchParams searchParams = CustomerSearchParams.builder()
                    .setQuery("metadata['userId']:'" + userId + "'")
                    .build();
            
            CustomerSearchResult searchResult = Customer.search(searchParams);
            
            if (!searchResult.getData().isEmpty()) {
                return searchResult.getData().get(0);
            }
            
            // Créer un nouveau client
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .putMetadata("userId", userId)
                    .build();
            
            return Customer.create(params);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(error -> log.error("Error creating/getting Stripe customer: ", error));
    }

    // ========== Checkout Session ==========

    public Mono<CheckoutSessionDto> createCheckoutSession(String userId, CheckoutRequestDto request) {
        return createOrGetCustomer(userId, request.getEmail())
                .flatMap(customer -> createStripeCheckoutSession(customer, request))
                .map(this::toCheckoutSessionDto)
                .doOnSuccess(session -> log.info("Checkout session created: {}", session.getId()));
    }

    private Mono<Session> createStripeCheckoutSession(Customer customer, CheckoutRequestDto request) {
        return Mono.fromCallable(() -> {
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setCustomer(customer.getId())
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(stripeConfig.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(stripeConfig.getCancelUrl())
                    .putMetadata("planId", request.getPlanId());
            
            // Ajouter les items de ligne
            SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                    .setPrice(request.getPriceId())
                    .setQuantity(1L)
                    .build();
            
            paramsBuilder.addLineItem(lineItem);
            
            // Options d'abonnement
            SessionCreateParams.SubscriptionData subscriptionData = 
                    SessionCreateParams.SubscriptionData.builder()
                            .putMetadata("userId", customer.getMetadata().get("userId"))
                            .putMetadata("planId", request.getPlanId())
                            .build();
            
            paramsBuilder.setSubscriptionData(subscriptionData);
            
            // Période d'essai si applicable
            if (request.getTrialDays() > 0) {
                subscriptionData = subscriptionData.toBuilder()
                        .setTrialPeriodDays((long) request.getTrialDays())
                        .build();
                paramsBuilder.setSubscriptionData(subscriptionData);
            }
            
            // Collecter l'adresse de facturation
            paramsBuilder.setBillingAddressCollection(
                    SessionCreateParams.BillingAddressCollection.REQUIRED);
            
            // Promotion/Coupon
            if (request.getCouponCode() != null) {
                paramsBuilder.addDiscount(
                        SessionCreateParams.Discount.builder()
                                .setCoupon(request.getCouponCode())
                                .build()
                );
            }
            
            return Session.create(paramsBuilder.build());
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    // ========== Subscription Management ==========

    public Mono<Subscription> createSubscriptionDirectly(String customerId, String priceId, String planId) {
        return Mono.fromCallable(() -> {
            SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                    .setCustomer(customerId)
                    .addItem(SubscriptionCreateParams.Item.builder()
                            .setPrice(priceId)
                            .build())
                    .putMetadata("planId", planId)
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                    .addAllExpand(Arrays.asList("latest_invoice.payment_intent"))
                    .build();
            
            return com.stripe.model.Subscription.create(params);
        })
        .flatMap(stripeSubscription -> saveSubscription(stripeSubscription))
        .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> cancelSubscription(String subscriptionId, boolean immediately) {
        return Mono.fromCallable(() -> {
            com.stripe.model.Subscription subscription = 
                    com.stripe.model.Subscription.retrieve(subscriptionId);
            
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(!immediately)
                    .build();
            
            if (immediately) {
                subscription.cancel();
            } else {
                subscription.update(params);
            }
            
            return subscription;
        })
        .flatMap(this::updateSubscriptionStatus)
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    // ========== Payment Intent ==========

    public Mono<PaymentIntent> createPaymentIntent(BigDecimal amount, String currency, 
                                                   String customerId, Map<String, String> metadata) {
        return Mono.fromCallable(() -> {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue()) // Cents
                    .setCurrency(currency)
                    .setCustomer(customerId)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .build();
            
            return PaymentIntent.create(params);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    // ========== Webhook Handling ==========

    public Mono<Event> handleWebhook(String payload, String signature) {
        return Mono.fromCallable(() -> {
            Event event = Webhook.constructEvent(
                    payload, 
                    signature, 
                    stripeConfig.getWebhookSecret()
            );
            
            log.info("Received Stripe webhook event: {}", event.getType());
            
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "customer.subscription.created":
                case "customer.subscription.updated":
                    handleSubscriptionUpdate(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                default:
                    log.info("Unhandled event type: {}", event.getType());
            }
            
            return event;
        })
        .flatMap(event -> webhookService.recordEvent(event))
        .subscribeOn(Schedulers.boundedElastic());
    }

    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow();
        
        String userId = session.getMetadata().get("userId");
        String planId = session.getMetadata().get("planId");
        
        log.info("Checkout completed for user: {} with plan: {}", userId, planId);
        
        // Activer l'abonnement
        activateSubscription(userId, session.getSubscription(), planId).subscribe();
    }

    private void handleSubscriptionUpdate(Event event) {
        com.stripe.model.Subscription subscription = 
                (com.stripe.model.Subscription) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow();
        
        updateSubscriptionStatus(subscription).subscribe();
    }

    // ========== Helper Methods ==========

    private CheckoutSessionDto toCheckoutSessionDto(Session session) {
        return CheckoutSessionDto.builder()
                .sessionId(session.getId())
                .checkoutUrl(session.getUrl())
                .status(session.getStatus())
                .expiresAt(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(session.getExpiresAt()), 
                        ZoneId.systemDefault()
                ))
                .build();
    }

    private Mono<Subscription> saveSubscription(com.stripe.model.Subscription stripeSubscription) {
        Subscription subscription = new Subscription();
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStatus(mapStripeStatus(stripeSubscription.getStatus()));
        subscription.setCurrentPeriodStart(
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodStart()),
                        ZoneId.systemDefault()
                )
        );
        subscription.setCurrentPeriodEnd(
                LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()),
                        ZoneId.systemDefault()
                )
        );
        
        return subscriptionRepository.save(subscription);
    }

    private SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELLED;
            case "incomplete" -> SubscriptionStatus.PENDING;
            case "trialing" -> SubscriptionStatus.TRIAL;
            default -> SubscriptionStatus.PENDING;
        };
    }
}