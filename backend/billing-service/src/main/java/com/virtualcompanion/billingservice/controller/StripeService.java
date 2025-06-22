package com.virtualcompanion.billingservice.controller;

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
