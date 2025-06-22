package com.virtualcompanion.billing.service;

class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private StripeService stripeService;

    @Mock
    private UsageTrackingService usageService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Subscription testSubscription;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        testSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tier(SubscriptionTier.PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .stripeSubscriptionId("sub_123")
                .build();
    }

    @Test
    @DisplayName("Should create subscription successfully")
    void createSubscription_Success() throws Exception {
        // Given
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setTier(SubscriptionTier.PREMIUM);
        request.setPaymentMethodId("pm_123");

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn("cus_123");
        
        com.stripe.model.Subscription mockStripeSubscription = mock(com.stripe.model.Subscription.class);
        when(mockStripeSubscription.getId()).thenReturn("sub_123");

        when(stripeService.createCustomer(any())).thenReturn(mockCustomer);
        when(stripeService.createSubscription(any())).thenReturn(mockStripeSubscription);
        when(subscriptionRepository.save(any())).thenReturn(testSubscription);

        // When
        SubscriptionResponse response = subscriptionService.createSubscription(request, userId.toString());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTier()).isEqualTo(SubscriptionTier.PREMIUM);
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        
        verify(stripeService).createCustomer(any());
        verify(stripeService).createSubscription(any());
        verify(subscriptionRepository).save(any());
    }

    @Test
    @DisplayName("Should handle subscription cancellation")
    void cancelSubscription_Success() throws Exception {
        // Given
        when(subscriptionRepository.findByUserIdAndStatus(any(), any()))
                .thenReturn(Optional.of(testSubscription));
        
        com.stripe.model.Subscription cancelledSub = mock(com.stripe.model.Subscription.class);
        when(stripeService.cancelSubscription(anyString())).thenReturn(cancelledSub);

        // When
        subscriptionService.cancelSubscription(userId.toString());

        // Then
        verify(stripeService).cancelSubscription("sub_123");
        verify(subscriptionRepository).save(argThat(sub -> 
            sub.getStatus() == SubscriptionStatus.CANCELLED
        ));
    }

    @Test
    @DisplayName("Should track usage correctly")
    void trackUsage_Success() {
        // Given
        when(subscriptionRepository.findByUserIdAndStatus(any(), any()))
                .thenReturn(Optional.of(testSubscription));

        UsageEvent event = UsageEvent.builder()
                .userId(userId)
                .type(UsageType.MESSAGE)
                .amount(1)
                .build();

        // When
        subscriptionService.trackUsage(event);

        // Then
        verify(usageService).recordUsage(event);
    }

    @Test
    @DisplayName("Should handle payment failure")
    void handlePaymentFailure() {
        // Given
        when(subscriptionRepository.findByStripeSubscriptionId(anyString()))
                .thenReturn(Optional.of(testSubscription));

        // When
        subscriptionService.handlePaymentFailure("sub_123");

        // Then
        verify(subscriptionRepository).save(argThat(sub -> 
            sub.getStatus() == SubscriptionStatus.PAST_DUE
        ));
    }

    @Test
    @DisplayName("Should enforce usage limits")
    void enforceUsageLimits() {
        // Given
        testSubscription.setTier(SubscriptionTier.FREE);
        
        when(subscriptionRepository.findByUserIdAndStatus(any(), any()))
                .thenReturn(Optional.of(testSubscription));
        when(usageService.getCurrentUsage(any(), any()))
                .thenReturn(100L); // At limit

        // When & Then
        assertThatThrownBy(() -> 
            subscriptionService.checkUsageLimit(userId.toString(), UsageType.MESSAGE)
        )
        .isInstanceOf(UsageLimitExceededException.class)
        .hasMessage("Usage limit exceeded for MESSAGE");
    }
}
