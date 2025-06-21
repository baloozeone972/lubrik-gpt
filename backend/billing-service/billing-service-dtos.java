// CreateSubscriptionRequest.java
package com.virtualcompanion.billingservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {
    
    @NotNull(message = "Plan is required")
    private String plan; // standard, premium, vip
    
    private String paymentMethodId; // Stripe payment method ID
    
    private String paymentProvider; // stripe, paypal
    
    private Boolean startTrial;
    
    private Map<String, Object> metadata;
}

// SubscriptionResponse.java
package com.virtualcompanion.billingservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionResponse {
    
    private UUID id;
    private UUID userId;
    private String plan;
    private String status; // active, trialing, past_due, canceled, suspended
    private BigDecimal price;
    private String currency;
    private String billingCycle; // monthly, yearly
    private LocalDateTime startDate;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime trialEndDate;
    private LocalDateTime canceledAt;
    private Boolean autoRenew;
    private String paymentProvider;
    private String externalSubscriptionId;
    private SubscriptionLimits limits;
    private Map<String, Object> metadata;
}

// SubscriptionLimits.java
package com.virtualcompanion.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionLimits {
    
    private Integer maxCharacters;
    private Integer maxConversationsPerDay;
    private Boolean voiceGeneration;
    private Boolean videoChat;
    private Boolean customCharacters;
    private Integer voiceMinutesPerMonth;
    private Integer videoMinutesPerMonth;
    private Boolean prioritySupport;
    private Boolean earlyAccess;
}

// UpdateSubscriptionRequest.java
package com.virtualcompanion.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubscriptionRequest {
    
    private String plan; // for upgrades/downgrades
    
    private Boolean autoRenew;
    
    private String paymentMethodId; // to update payment method
    
    private Boolean cancelAtPeriodEnd;
}

// PaymentRequest.java
package com.virtualcompanion.billingservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @NotNull(message = "Currency is required")
    private String currency;
    
    private String description;
    
    @NotNull(message = "Payment method is required")
    private String paymentMethodId;
    
    private String paymentProvider; // stripe, paypal
    
    private Map<String, Object> metadata;
}

// PaymentResponse.java
package com.virtualcompanion.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private UUID id;
    private UUID userId;
    private UUID subscriptionId;
    private BigDecimal amount;
    private String currency;
    private String status; // pending, processing, succeeded, failed, refunded
    private String paymentProvider;
    private String externalPaymentId;
    private String paymentMethodType;
    private String paymentMethodLast4;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Map<String, Object> metadata;
}

// CreatePaymentMethodRequest.java
package com.virtualcompanion.billingservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentMethodRequest {
    
    @NotNull(message = "Payment provider is required")
    private String paymentProvider; // stripe, paypal
    
    private String paymentMethodId; // For Stripe
    
    private String paypalEmail; // For PayPal
    
    private Boolean setAsDefault;
}

// PaymentMethodResponse.java
package com.virtualcompanion.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

// InvoiceResponse.java
package com.virtualcompanion.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    
    private UUID id;
    private String invoiceNumber;
    private UUID userId;
    private UUID subscriptionId;
    private String status; // draft, open, paid, void, uncollectible
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private String currency;
    private LocalDateTime invoiceDate;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private String paymentProvider;
    private String externalInvoiceId;
    private String downloadUrl;
    private List<InvoiceLineItem> lineItems;
}

// InvoiceLineItem.java
package com.virtualcompanion.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineItem {
    
    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String type; // subscription, usage, credit, discount
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}

// UsageRecordRequest.java
package com.virtualcompanion.billingservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecordRequest {
    
    @NotNull(message = "Usage type is required")
    private String usageType; // conversation, voice_minutes, video_minutes, storage
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantity;
    
    private UUID characterId;
    
    private LocalDateTime timestamp;
    
    private Map<String, Object> metadata;
}

// UsageStatisticsResponse.java
package com.virtualcompanion.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatisticsResponse {
    
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private Map<String, UsageDetail> usage;
    private SubscriptionLimits limits;
    private Map<String, Double> usagePercentage;
}

// UsageDetail.java
package com.virtualcompanion.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageDetail {
    
    private Double used;
    private Double limit;
    private String unit;
    private Boolean overage;
    private Double overageAmount;
}

// RefundRequest.java
package com.virtualcompanion.billingservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    
    @NotNull(message = "Payment ID is required")
    private UUID paymentId;
    
    @Positive(message = "Amount must be positive")
    private BigDecimal amount; // null for full refund
    
    @NotNull(message = "Reason is required")
    private String reason;
    
    private String internalNotes;
}

// RefundResponse.java
package com.virtualcompanion.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    
    private UUID id;
    private UUID paymentId;
    private BigDecimal amount;
    private String currency;
    private String status; // pending, succeeded, failed, canceled
    private String reason;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}

// BillingEventRequest.java
package com.virtualcompanion.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingEventRequest {
    
    private String eventType;
    private UUID userId;
    private UUID resourceId;
    private String resourceType;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
}

// WebhookPayload.java
package com.virtualcompanion.billingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {
    
    private String provider; // stripe, paypal
    private String eventType;
    private String eventId;
    private Map<String, Object> data;
    private String signature;
}