package com.virtualcompanion.billingservice.dto;

public class UpdateSubscriptionRequest {
    
    private String plan; // for upgrades/downgrades
    
    private Boolean autoRenew;
    
    private String paymentMethodId; // to update payment method
    
    private Boolean cancelAtPeriodEnd;
}
