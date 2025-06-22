package com.virtualcompanion.billingservice.service;

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
