// SubscriptionController.java
package com.virtualcompanion.billingservice.controller;

import com.virtualcompanion.billingservice.dto.*;
import com.virtualcompanion.billingservice.service.SubscriptionService;
import com.virtualcompanion.billingservice.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscription management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    private final UsageService usageService;
    
    @PostMapping
    @Operation(summary = "Create a new subscription")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        
        SubscriptionResponse subscription = subscriptionService.createSubscription(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }
    
    @GetMapping("/current")
    @Operation(summary = "Get current subscription")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription(
            @AuthenticationPrincipal UUID userId) {
        
        SubscriptionResponse subscription = subscriptionService.getSubscription(userId);
        return ResponseEntity.ok(subscription);
    }
    
    @PutMapping("/current")
    @Operation(summary = "Update current subscription")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        
        SubscriptionResponse subscription = subscriptionService.updateSubscription(userId, request);
        return ResponseEntity.ok(subscription);
    }
    
    @PostMapping("/current/cancel")
    @Operation(summary = "Cancel subscription")
    public ResponseEntity<Void> cancelSubscription(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "false") boolean immediately) {
        
        subscriptionService.cancelSubscription(userId, immediately);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/current/reactivate")
    @Operation(summary = "Reactivate canceled subscription")
    public ResponseEntity<Void> reactivateSubscription(
            @AuthenticationPrincipal UUID userId) {
        
        subscriptionService.reactivateSubscription(userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/history")
    @Operation(summary = "Get subscription history")
    public ResponseEntity<Page<SubscriptionResponse>> getSubscriptionHistory(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SubscriptionResponse> history = subscriptionService.getUserSubscriptionHistory(userId, pageRequest);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/limits")
    @Operation(summary = "Get subscription limits")
    public ResponseEntity<SubscriptionLimits> getSubscriptionLimits(
            @AuthenticationPrincipal UUID userId) {
        
        SubscriptionLimits limits = subscriptionService.getSubscriptionLimits(userId);
        return ResponseEntity.ok(limits);
    }
    
    @GetMapping("/usage")
    @Operation(summary = "Get current usage statistics")
    public ResponseEntity<UsageStatisticsResponse> getUsageStatistics(
            @AuthenticationPrincipal UUID userId) {
        
        UsageStatisticsResponse usage = usageService.getCurrentPeriodUsage(userId);
        return ResponseEntity.ok(usage);
    }
    
    @PostMapping("/usage")
    @Operation(summary = "Record usage")
    public ResponseEntity<Void> recordUsage(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UsageRecordRequest request) {
        
        usageService.recordUsage(userId, request);
        return ResponseEntity.accepted().build();
    }
}

// PaymentController.java
package com.virtualcompanion.billingservice.controller;

import com.virtualcompanion.billingservice.dto.*;
import com.virtualcompanion.billingservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping
    @Operation(summary = "Process a payment")
    public ResponseEntity<PaymentResponse> processPayment(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody PaymentRequest request) {
        
        PaymentResponse payment = paymentService.processPayment(userId, request);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details")
    public ResponseEntity<PaymentResponse> getPayment(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID paymentId) {
        
        PaymentResponse payment = paymentService.getPayment(userId, paymentId);
        return ResponseEntity.ok(payment);
    }
    
    @GetMapping
    @Operation(summary = "Get user's payment history")
    public ResponseEntity<Page<PaymentResponse>> getPaymentHistory(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PaymentResponse> payments = paymentService.getUserPayments(userId, pageRequest);
        return ResponseEntity.ok(payments);
    }
    
    @PostMapping("/refund")
    @Operation(summary = "Request a refund")
    public ResponseEntity<RefundResponse> requestRefund(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RefundRequest request) {
        
        RefundResponse refund = paymentService.processRefund(userId, request);
        return ResponseEntity.ok(refund);
    }
    
    @GetMapping("/methods")
    @Operation(summary = "Get saved payment methods")
    public ResponseEntity<List<PaymentMethodResponse>> getPaymentMethods(
            @AuthenticationPrincipal UUID userId) {
        
        List<PaymentMethodResponse> methods = paymentService.getUserPaymentMethods(userId);
        return ResponseEntity.ok(methods);
    }
    
    @PostMapping("/methods")
    @Operation(summary = "Add a payment method")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PaymentMethodResponse> addPaymentMethod(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreatePaymentMethodRequest request) {
        
        PaymentMethodResponse method = paymentService.addPaymentMethod(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(method);
    }
    
    @DeleteMapping("/methods/{methodId}")
    @Operation(summary = "Remove a payment method")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePaymentMethod(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID methodId) {
        
        paymentService.removePaymentMethod(userId, methodId);
    }
    
    @PostMapping("/methods/{methodId}/set-default")
    @Operation(summary = "Set default payment method")
    public ResponseEntity<Void> setDefaultPaymentMethod(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID methodId) {
        
        paymentService.setDefaultPaymentMethod(userId, methodId);
        return ResponseEntity.ok().build();
    }
}

// InvoiceController.java
package com.virtualcompanion.billingservice.controller;

import com.virtualcompanion.billingservice.dto.InvoiceResponse;
import com.virtualcompanion.billingservice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class InvoiceController {
    
    private final InvoiceService invoiceService;
    
    @GetMapping
    @Operation(summary = "Get user's invoices")
    public ResponseEntity<Page<InvoiceResponse>> getUserInvoices(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "invoiceDate"));
        Page<InvoiceResponse> invoices = invoiceService.getUserInvoices(userId, pageRequest);
        return ResponseEntity.ok(invoices);
    }
    
    @GetMapping("/{invoiceId}")
    @Operation(summary = "Get invoice details")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID invoiceId) {
        
        InvoiceResponse invoice = invoiceService.getInvoice(userId, invoiceId);
        return ResponseEntity.ok(invoice);
    }
    
    @GetMapping("/{invoiceId}/download")
    @Operation(summary = "Download invoice PDF")
    public ResponseEntity<Resource> downloadInvoice(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID invoiceId) {
        
        byte[] pdfContent = invoiceService.generateInvoicePdf(userId, invoiceId);
        ByteArrayResource resource = new ByteArrayResource(pdfContent);
        
        String filename = "invoice_" + invoiceId + ".pdf";
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(pdfContent.length)
                .body(resource);
    }
    
    @PostMapping("/{invoiceId}/send")
    @Operation(summary = "Send invoice by email")
    public ResponseEntity<Void> sendInvoiceByEmail(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID invoiceId) {
        
        invoiceService.sendInvoiceByEmail(userId, invoiceId);
        return ResponseEntity.ok().build();
    }
}

// WebhookController.java
package com.virtualcompanion.billingservice.controller;

import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.virtualcompanion.billingservice.dto.WebhookPayload;
import com.virtualcompanion.billingservice.service.SubscriptionService;
import com.virtualcompanion.billingservice.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Payment provider webhook endpoints")
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

// AdminBillingController.java
package com.virtualcompanion.billingservice.controller;

import com.virtualcompanion.billingservice.dto.BillingReportRequest;
import com.virtualcompanion.billingservice.dto.BillingReportResponse;
import com.virtualcompanion.billingservice.service.BillingReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/billing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Billing", description = "Administrative billing endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AdminBillingController {
    
    private final BillingReportService reportService;
    
    @PostMapping("/reports/revenue")
    @Operation(summary = "Generate revenue report")
    public ResponseEntity<BillingReportResponse> generateRevenueReport(
            @Valid @RequestBody BillingReportRequest request) {
        
        BillingReportResponse report = reportService.generateRevenueReport(request);
        return ResponseEntity.ok(report);
    }
    
    @PostMapping("/reports/subscriptions")
    @Operation(summary = "Generate subscription report")
    public ResponseEntity<BillingReportResponse> generateSubscriptionReport(
            @Valid @RequestBody BillingReportRequest request) {
        
        BillingReportResponse report = reportService.generateSubscriptionReport(request);
        return ResponseEntity.ok(report);
    }
    
    @PostMapping("/reports/usage")
    @Operation(summary = "Generate usage report")
    public ResponseEntity<BillingReportResponse> generateUsageReport(
            @Valid @RequestBody BillingReportRequest request) {
        
        BillingReportResponse report = reportService.generateUsageReport(request);
        return ResponseEntity.ok(report);
    }
    
    @PostMapping("/jobs/process-renewals")
    @Operation(summary = "Manually trigger subscription renewals")
    public ResponseEntity<Void> triggerRenewalProcessing() {
        reportService.triggerRenewalProcessing();
        return ResponseEntity.accepted().build();
    }
    
    @PostMapping("/jobs/process-trials")
    @Operation(summary = "Manually trigger trial conversions")
    public ResponseEntity<Void> triggerTrialProcessing() {
        reportService.triggerTrialProcessing();
        return ResponseEntity.accepted().build();
    }
}