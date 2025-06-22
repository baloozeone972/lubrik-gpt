package com.virtualcompanion.billingservice.dto;

public class PaymentMethodResponse {

    private UUID id;
    private UUID userId;
    private String paymentProvider;
    private String externalMethodId;
    private String type; // card, bank_account, paypal
    private String brand; // visa, mastercard, etc.
    private String last4;
    private String email; // for PayPal
    private Integer expMonth;
    private Integer expYear;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
