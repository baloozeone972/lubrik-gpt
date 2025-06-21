// SubscriptionService.java
package com.virtualcompanion.billingservice.service;

import com.virtualcompanion.billingservice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SubscriptionService {
    SubscriptionResponse createSubscription(UUID userId, CreateSubscriptionRequest request);
    SubscriptionResponse getSubscription(UUID userId);
    SubscriptionResponse updateSubscription(UUID userId, UpdateSubscriptionRequest request);
    void cancelSubscription(UUID userId, boolean immediately);
    void reactivateSubscription(UUID userId);
    Page<SubscriptionResponse> getUserSubscriptionHistory(UUID userId, Pageable pageable);
    SubscriptionLimits getSubscriptionLimits(UUID userId);
    void processSubscriptionRenewals();
    void processTrialConversions();
    void handleSubscriptionWebhook(String provider, WebhookPayload payload);
}

// SubscriptionServiceImpl.java
package com.virtualcompanion.billingservice.service.impl;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.virtualcompanion.billingservice.client.UserServiceClient;
import com.virtualcompanion.billingservice.config.SubscriptionPlanConfig;
import com.virtualcompanion.billingservice.dto.*;
import com.virtualcompanion.billingservice.entity.BillingEvent;
import com.virtualcompanion.billingservice.entity.Payment;
import com.virtualcompanion.billingservice.exception.BillingException;
import com.virtualcompanion.billingservice.exception.SubscriptionNotFoundException;
import com.virtualcompanion.billingservice.mapper.SubscriptionMapper;
import com.virtualcompanion.billingservice.repository.BillingEventRepository;
import com.virtualcompanion.billingservice.repository.PaymentRepository;
import com.virtualcompanion.billingservice.repository.SubscriptionRepository;
import com.virtualcompanion.billingservice.service.PaymentService;
import com.virtualcompanion.billingservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {
    
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final BillingEventRepository billingEventRepository;
    private final UserServiceClient userServiceClient;
    private final PaymentService paymentService;
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionPlanConfig planConfig;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${stripe.api-key}")
    private String stripeApiKey;
    
    @Value("${stripe.products}")
    private Map<String, Map<String, String>> stripeProducts;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }
    
    @Override
    public SubscriptionResponse createSubscription(UUID userId, CreateSubscriptionRequest request) {
        log.info("Creating subscription for user: {} plan: {}", userId, request.getPlan());
        
        // Check if user already has active subscription
        subscriptionRepository.findActiveSubscriptionByUserId(userId).ifPresent(existing -> {
            throw new BillingException("User already has an active subscription");
        });
        
        try {
            // Get or create Stripe customer
            String customerId = getOrCreateStripeCustomer(userId);
            
            // Get plan details
            var planDetails = planConfig.getPlans().get(request.getPlan());
            if (planDetails == null) {
                throw new BillingException("Invalid subscription plan: " + request.getPlan());
            }
            
            // Create Stripe subscription
            String priceId = stripeProducts.get(request.getPlan()).get("price-id");
            
            SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                    .setCustomer(customerId)
                    .addItem(SubscriptionCreateParams.Item.builder()
                            .setPrice(priceId)
                            .build())
                    .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                    .addExpand("latest_invoice.payment_intent");
            
            // Add payment method if provided
            if (request.getPaymentMethodId() != null) {
                paramsBuilder.setDefaultPaymentMethod(request.getPaymentMethodId());
            }
            
            // Add trial period if applicable
            if (Boolean.TRUE.equals(request.getStartTrial()) && planDetails.getTrialDays() > 0) {
                paramsBuilder.setTrialPeriodDays((long) planDetails.getTrialDays());
            }
            
            // Add metadata
            paramsBuilder.putMetadata("user_id", userId.toString());
            if (request.getMetadata() != null) {
                request.getMetadata().forEach((k, v) -> paramsBuilder.putMetadata(k, v.toString()));
            }
            
            Subscription stripeSubscription = Subscription.create(paramsBuilder.build());
            
            // Create local subscription record
            com.virtualcompanion.billingservice.entity.Subscription subscription = 
                com.virtualcompanion.billingservice.entity.Subscription.builder()
                    .userId(userId)
                    .plan(request.getPlan())
                    .status(mapStripeStatus(stripeSubscription.getStatus()))
                    .price(planDetails.getPrice())
                    .currency(planDetails.getCurrency())
                    .billingCycle("monthly")
                    .startDate(LocalDateTime.ofEpochSecond(stripeSubscription.getStartDate(), 0, ZoneOffset.UTC))
                    .currentPeriodStart(LocalDateTime.ofEpochSecond(stripeSubscription.getCurrentPeriodStart(), 0, ZoneOffset.UTC))
                    .currentPeriodEnd(LocalDateTime.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd(), 0, ZoneOffset.UTC))
                    .trialEndDate(stripeSubscription.getTrialEnd() != null ? 
                            LocalDateTime.ofEpochSecond(stripeSubscription.getTrialEnd(), 0, ZoneOffset.UTC) : null)
                    .autoRenew(true)
                    .paymentProvider("stripe")
                    .externalSubscriptionId(stripeSubscription.getId())
                    .externalCustomerId(customerId)
                    .metadata(request.getMetadata())
                    .build();
            
            subscription = subscriptionRepository.save(subscription);
            
            // Create subscription limits
            SubscriptionLimits limits = createSubscriptionLimits(subscription);
            subscription.setLimits(limits);
            
            // Record event
            recordBillingEvent(userId, subscription.getId(), "subscription", "subscription.created", 
                    Map.of("plan", request.getPlan()));
            
            // Update user service
            updateUserSubscription(userId, request.getPlan());
            
            // Publish event
            publishSubscriptionEvent(subscription, "created");
            
            log.info("Subscription created successfully: {}", subscription.getId());
            
            SubscriptionResponse response = subscriptionMapper.toResponse(subscription);
            response.setLimits(limits);
            return response;
            
        } catch (StripeException e) {
            log.error("Stripe error creating subscription: {}", e.getMessage());
            throw new BillingException("Failed to create subscription: " + e.getMessage());
        }
    }
    
    @Override
    public SubscriptionResponse getSubscription(UUID userId) {
        com.virtualcompanion.billingservice.entity.Subscription subscription = 
                subscriptionRepository.findActiveSubscriptionByUserId(userId)
                        .orElseThrow(() -> new SubscriptionNotFoundException("No active subscription found"));
        
        SubscriptionResponse response = subscriptionMapper.toResponse(subscription);
        response.setLimits(createSubscriptionLimits(subscription));
        return response;
    }
    
    @Override
    public SubscriptionResponse updateSubscription(UUID userId, UpdateSubscriptionRequest request) {
        com.virtualcompanion.billingservice.entity.Subscription subscription = 
                subscriptionRepository.findActiveSubscriptionByUserId(userId)
                        .orElseThrow(() -> new SubscriptionNotFoundException("No active subscription found"));
        
        try {
            Subscription stripeSubscription = Subscription.retrieve(subscription.getExternalSubscriptionId());
            SubscriptionUpdateParams.Builder updateParams = SubscriptionUpdateParams.builder();
            
            // Handle plan change
            if (request.getPlan() != null && !request.getPlan().equals(subscription.getPlan())) {
                String newPriceId = stripeProducts.get(request.getPlan()).get("price-id");
                updateParams.addItem(SubscriptionUpdateParams.Item.builder()
                        .setId(stripeSubscription.getItems().getData().get(0).getId())
                        .setPrice(newPriceId)
                        .build());
                
                subscription.setPlan(request.getPlan());
                
                // Update price
                var planDetails = planConfig.getPlans().get(request.getPlan());
                subscription.setPrice(planDetails.getPrice());
            }
            
            // Handle auto-renew
            if (request.getAutoRenew() != null) {
                updateParams.setCancelAtPeriodEnd(!request.getAutoRenew());
                subscription.setAutoRenew(request.getAutoRenew());
            }
            
            // Handle payment method update
            if (request.getPaymentMethodId() != null) {
                updateParams.setDefaultPaymentMethod(request.getPaymentMethodId());
            }
            
            // Handle cancel at period end
            if (Boolean.TRUE.equals(request.getCancelAtPeriodEnd())) {
                updateParams.setCancelAtPeriodEnd(true);
                subscription.setAutoRenew(false);
            }
            
            stripeSubscription = stripeSubscription.update(updateParams.build());
            
            subscription.setStatus(mapStripeStatus(stripeSubscription.getStatus()));
            subscription.setUpdatedAt(LocalDateTime.now());
            subscription = subscriptionRepository.save(subscription);
            
            // Record event
            recordBillingEvent(userId, subscription.getId(), "subscription", "subscription.updated", 
                    Map.of("changes", request));
            
            // Update user service if plan changed
            if (request.getPlan() != null) {
                updateUserSubscription(userId, request.getPlan());
            }
            
            // Publish event
            publishSubscriptionEvent(subscription, "updated");
            
            SubscriptionResponse response = subscriptionMapper.toResponse(subscription);
            response.setLimits(createSubscriptionLimits(subscription));
            return response;
            
        } catch (StripeException e) {
            log.error("Stripe error updating subscription: {}", e.getMessage());
            throw new BillingException("Failed to update subscription: " + e.getMessage());
        }
    }
    
    @Override
    public void cancelSubscription(UUID userId, boolean immediately) {
        com.virtualcompanion.billingservice.entity.Subscription subscription = 
                subscriptionRepository.findActiveSubscriptionByUserId(userId)
                        .orElseThrow(() -> new SubscriptionNotFoundException("No active subscription found"));
        
        try {
            Subscription stripeSubscription = Subscription.retrieve(subscription.getExternalSubscriptionId());
            
            if (immediately) {
                stripeSubscription = stripeSubscription.cancel();
                subscription.setStatus("canceled");
                subscription.setCanceledAt(LocalDateTime.now());
            } else {
                SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build();
                stripeSubscription = stripeSubscription.update(params);
                subscription.setAutoRenew(false);
            }
            
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            // Record event
            recordBillingEvent(userId, subscription.getId(), "subscription", "subscription.canceled", 
                    Map.of("immediate", immediately));
            
            // Update user service if immediate cancellation
            if (immediately) {
                updateUserSubscription(userId, "free");
            }
            
            // Publish event
            publishSubscriptionEvent(subscription, "canceled");
            
            log.info("Subscription canceled for user: {} immediate: {}", userId, immediately);
            
        } catch (StripeException e) {
            log.error("Stripe error canceling subscription: {}", e.getMessage());
            throw new BillingException("Failed to cancel subscription: " + e.getMessage());
        }
    }
    
    @Override
    public void reactivateSubscription(UUID userId) {
        com.virtualcompanion.billingservice.entity.Subscription subscription = 
                subscriptionRepository.findByUserId(userId)
                        .orElseThrow(() -> new SubscriptionNotFoundException("No subscription found"));
        
        if (!"canceled".equals(subscription.getStatus()) && subscription.getAutoRenew()) {
            throw new BillingException("Subscription is not canceled");
        }
        
        try {
            Subscription stripeSubscription = Subscription.retrieve(subscription.getExternalSubscriptionId());
            
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(false)
                    .build();
            
            stripeSubscription = stripeSubscription.update(params);
            
            subscription.setAutoRenew(true);
            subscription.setStatus(mapStripeStatus(stripeSubscription.getStatus()));
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            // Record event
            recordBillingEvent(userId, subscription.getId(), "subscription", "subscription.reactivated", null);
            
            // Update user service
            updateUserSubscription(userId, subscription.getPlan());
            
            // Publish event
            publishSubscriptionEvent(subscription, "reactivated");
            
            log.info("Subscription reactivated for user: {}", userId);
            
        } catch (StripeException e) {
            log.error("Stripe error reactivating subscription: {}", e.getMessage());
            throw new BillingException("Failed to reactivate subscription: " + e.getMessage());
        }
    }
    
    @Override
    public Page<SubscriptionResponse> getUserSubscriptionHistory(UUID userId, Pageable pageable) {
        // This would include all subscriptions, not just active ones
        return subscriptionRepository.findAll(pageable)
                .map(sub -> {
                    SubscriptionResponse response = subscriptionMapper.toResponse(sub);
                    response.setLimits(createSubscriptionLimits(sub));
                    return response;
                });
    }
    
    @Override
    public SubscriptionLimits getSubscriptionLimits(UUID userId) {
        com.virtualcompanion.billingservice.entity.Subscription subscription = 
                subscriptionRepository.findActiveSubscriptionByUserId(userId)
                        .orElse(null);
        
        if (subscription == null) {
            // Return free tier limits
            return createFreeTierLimits();
        }
        
        return createSubscriptionLimits(subscription);
    }
    
    @Override
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    public void processSubscriptionRenewals() {
        log.info("Processing subscription renewals");
        
        LocalDateTime now = LocalDateTime.now();
        List<com.virtualcompanion.billingservice.entity.Subscription> subscriptionsToRenew = 
                subscriptionRepository.findSubscriptionsToRenew(now);
        
        for (com.virtualcompanion.billingservice.entity.Subscription subscription : subscriptionsToRenew) {
            try {
                processRenewal(subscription);
            } catch (Exception e) {
                log.error("Failed to process renewal for subscription {}: {}", 
                        subscription.getId(), e.getMessage());
            }
        }
        
        log.info("Processed {} subscription renewals", subscriptionsToRenew.size());
    }
    
    @Override
    @Scheduled(cron = "0 0 3 * * *") // Run at 3 AM daily
    public void processTrialConversions() {
        log.info("Processing trial conversions");
        
        LocalDateTime now = LocalDateTime.now();
        List<com.virtualcompanion.billingservice.entity.Subscription> trialsToConvert = 
                subscriptionRepository.findTrialSubscriptionsToConvert(now);
        
        for (com.virtualcompanion.billingservice.entity.Subscription subscription : trialsToConvert) {
            try {
                processTrialConversion(subscription);
            } catch (Exception e) {
                log.error("Failed to process trial conversion for subscription {}: {}", 
                        subscription.getId(), e.getMessage());
            }
        }
        
        log.info("Processed {} trial conversions", trialsToConvert.size());
    }
    
    @Override
    public void handleSubscriptionWebhook(String provider, WebhookPayload payload) {
        log.info("Handling {} webhook: {}", provider, payload.getEventType());
        
        if ("stripe".equals(provider)) {
            handleStripeWebhook(payload);
        } else if ("paypal".equals(provider)) {
            handlePayPalWebhook(payload);
        } else {
            log.warn("Unknown webhook provider: {}", provider);
        }
    }
    
    private String getOrCreateStripeCustomer(UUID userId) throws StripeException {
        // Check if user already has a Stripe customer ID
        UserDetails userDetails = userServiceClient.getUser(userId);
        
        if (userDetails.getStripeCustomerId() != null) {
            return userDetails.getStripeCustomerId();
        }
        
        // Create new Stripe customer
        Map<String, Object> customerParams = new HashMap<>();
        customerParams.put("email", userDetails.getEmail());
        customerParams.put("name", userDetails.getFirstName() + " " + userDetails.getLastName());
        customerParams.put("metadata", Map.of("user_id", userId.toString()));
        
        Customer customer = Customer.create(customerParams);
        
        // Update user service with customer ID
        userServiceClient.updateStripeCustomerId(userId, customer.getId());
        
        return customer.getId();
    }
    
    private String mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> "active";
            case "trialing" -> "trialing";
            case "past_due" -> "past_due";
            case "canceled" -> "canceled";
            case "unpaid" -> "suspended";
            default -> "unknown";
        };
    }
    
    private SubscriptionLimits createSubscriptionLimits(com.virtualcompanion.billingservice.entity.Subscription subscription) {
        var planFeatures = planConfig.getPlans().get(subscription.getPlan()).getFeatures();
        
        return SubscriptionLimits.builder()
                .maxCharacters(planFeatures.get("max-characters"))
                .maxConversationsPerDay(planFeatures.get("max-conversations-per-day"))
                .voiceGeneration(planFeatures.get("voice-generation"))
                .videoChat(planFeatures.get("video-chat"))
                .customCharacters(planFeatures.get("custom-characters"))
                .voiceMinutesPerMonth(planFeatures.get("voice-minutes-per-month"))
                .videoMinutesPerMonth(planFeatures.get("video-minutes-per-month"))
                .prioritySupport(planFeatures.get("priority-support"))
                .earlyAccess(planFeatures.get("early-access"))
                .build();
    }
    
    private SubscriptionLimits createFreeTierLimits() {
        var planFeatures = planConfig.getPlans().get("free").getFeatures();
        
        return SubscriptionLimits.builder()
                .maxCharacters(planFeatures.get("max-characters"))
                .maxConversationsPerDay(planFeatures.get("max-conversations-per-day"))
                .voiceGeneration(false)
                .videoChat(false)
                .customCharacters(false)
                .voiceMinutesPerMonth(0)
                .videoMinutesPerMonth(0)
                .prioritySupport(false)
                .earlyAccess(false)
                .build();
    }
    
    private void updateUserSubscription(UUID userId, String plan) {
        try {
            userServiceClient.updateUserSubscription(userId, plan);
        } catch (Exception e) {
            log.error("Failed to update user subscription: {}", e.getMessage());
        }
    }
    
    private void recordBillingEvent(UUID userId, UUID resourceId, String resourceType, 
                                   String eventType, Map<String, Object> data) {
        BillingEvent event = BillingEvent.builder()
                .userId(userId)
                .resourceId(resourceId)
                .resourceType(resourceType)
                .eventType(eventType)
                .data(data)
                .build();
        
        billingEventRepository.save(event);
    }
    
    private void publishSubscriptionEvent(com.virtualcompanion.billingservice.entity.Subscription subscription, 
                                         String action) {
        Map<String, Object> event = Map.of(
                "subscriptionId", subscription.getId(),
                "userId", subscription.getUserId(),
                "plan", subscription.getPlan(),
                "status", subscription.getStatus(),
                "action", action,
                "timestamp", LocalDateTime.now()
        );
        
        kafkaTemplate.send("billing-events", "subscription." + action, event);
    }
    
    private void processRenewal(com.virtualcompanion.billingservice.entity.Subscription subscription) {
        // This would handle the renewal process
        log.info("Processing renewal for subscription: {}", subscription.getId());
    }
    
    private void processTrialConversion(com.virtualcompanion.billingservice.entity.Subscription subscription) {
        // This would handle trial to paid conversion
        log.info("Processing trial conversion for subscription: {}", subscription.getId());
    }
    
    private void handleStripeWebhook(WebhookPayload payload) {
        // Handle various Stripe webhook events
        switch (payload.getEventType()) {
            case "customer.subscription.updated":
                handleStripeSubscriptionUpdated(payload);
                break;
            case "customer.subscription.deleted":
                handleStripeSubscriptionDeleted(payload);
                break;
            case "invoice.payment_succeeded":
                handleStripePaymentSucceeded(payload);
                break;
            case "invoice.payment_failed":
                handleStripePaymentFailed(payload);
                break;
            default:
                log.debug("Unhandled Stripe webhook event: {}", payload.getEventType());
        }
    }
    
    private void handlePayPalWebhook(WebhookPayload payload) {
        // Handle PayPal webhook events
        log.info("Processing PayPal webhook: {}", payload.getEventType());
    }
    
    private void handleStripeSubscriptionUpdated(WebhookPayload payload) {
        // Implementation
    }
    
    private void handleStripeSubscriptionDeleted(WebhookPayload payload) {
        // Implementation
    }
    
    private void handleStripePaymentSucceeded(WebhookPayload payload) {
        // Implementation
    }
    
    private void handleStripePaymentFailed(WebhookPayload payload) {
        // Implementation
    }
}