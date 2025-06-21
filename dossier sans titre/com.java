package com.virtualcompanion.billingservice.service.impl;

record
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
            
            log.info("Subscription created successfully: {}
