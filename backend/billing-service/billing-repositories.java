// SubscriptionRepository.java
package com.virtualcompanion.billingservice.repository;

import com.virtualcompanion.billingservice.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    
    Optional<Subscription> findByUserId(UUID userId);
    
    Optional<Subscription> findByUserIdAndStatus(UUID userId, String status);
    
    Optional<Subscription> findByExternalSubscriptionId(String externalSubscriptionId);
    
    @Query("SELECT s FROM Subscription s WHERE s.status IN ('active', 'trialing') AND s.userId = :userId")
    Optional<Subscription> findActiveSubscriptionByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT s FROM Subscription s WHERE s.status = :status")
    Page<Subscription> findByStatus(@Param("status") String status, Pageable pageable);
    
    @Query("SELECT s FROM Subscription s WHERE s.currentPeriodEnd <= :date AND s.status = 'active' AND s.autoRenew = true")
    List<Subscription> findSubscriptionsToRenew(@Param("date") LocalDateTime date);
    
    @Query("SELECT s FROM Subscription s WHERE s.trialEndDate <= :date AND s.status = 'trialing'")
    List<Subscription> findTrialSubscriptionsToConvert(@Param("date") LocalDateTime date);
    
    @Query("SELECT s FROM Subscription s WHERE s.status = 'past_due' AND s.updatedAt <= :threshold")
    List<Subscription> findPastDueSubscriptions(@Param("threshold") LocalDateTime threshold);
    
    @Modifying
    @Query("UPDATE Subscription s SET s.status = :status, s.updatedAt = :now WHERE s.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") String status, @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.plan = :plan AND s.status IN ('active', 'trialing')")
    long countActiveSubscriptionsByPlan(@Param("plan") String plan);
    
    @Query("SELECT s.plan, COUNT(s) FROM Subscription s WHERE s.status IN ('active', 'trialing') GROUP BY s.plan")
    List<Object[]> getSubscriptionDistribution();
}

// PaymentRepository.java
package com.virtualcompanion.billingservice.repository;

import com.virtualcompanion.billingservice.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    Optional<Payment> findByExternalPaymentId(String externalPaymentId);
    
    Page<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    Page<Payment> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId, Pageable pageable);
    
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.status = :status")
    List<Payment> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.userId = :userId AND p.status = 'succeeded' " +
           "AND p.createdAt >= :startDate AND p.createdAt <= :endDate")
    BigDecimal getTotalPaymentsByUserInPeriod(@Param("userId") UUID userId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM Payment p WHERE p.status = 'pending' AND p.createdAt <= :threshold")
    List<Payment> findStuckPendingPayments(@Param("threshold") LocalDateTime threshold);
    
    @Modifying
    @Query("UPDATE Payment p SET p.status = :status, p.processedAt = :processedAt, " +
           "p.failureReason = :failureReason WHERE p.id = :id")
    void updatePaymentStatus(@Param("id") UUID id, 
                            @Param("status") String status,
                            @Param("processedAt") LocalDateTime processedAt,
                            @Param("failureReason") String failureReason);
    
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.subscriptionId = :subscriptionId AND p.status = 'failed' " +
           "AND p.createdAt >= :since")
    long countFailedPaymentsSince(@Param("subscriptionId") UUID subscriptionId, 
                                  @Param("since") LocalDateTime since);
}

// PaymentMethodRepository.java
package com.virtualcompanion.billingservice.repository;

import com.virtualcompanion.billingservice.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    
    List<PaymentMethod> findByUserIdAndIsActiveTrue(UUID userId);
    
    Optional<PaymentMethod> findByUserIdAndIsDefaultTrue(UUID userId);
    
    Optional<PaymentMethod> findByExternalMethodId(String externalMethodId);
    
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.userId = :userId AND pm.paymentProvider = :provider " +
           "AND pm.isActive = true")
    List<PaymentMethod> findByUserIdAndProvider(@Param("userId") UUID userId, @Param("provider") String provider);
    
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isDefault = false WHERE pm.userId = :userId")
    void clearDefaultForUser(@Param("userId") UUID userId);
    
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isActive = false WHERE pm.id = :id")
    void deactivatePaymentMethod(@Param("id") UUID id);
    
    boolean existsByUserIdAndExternalMethodId(UUID userId, String externalMethodId);
}

// InvoiceRepository.java
package com.virtualcompanion.billingservice.repository;

import com.virtualcompanion.billingservice.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    Optional<Invoice> findByExternalInvoiceId(String externalInvoiceId);
    
    Page<Invoice> findByUserIdOrderByInvoiceDateDesc(UUID userId, Pageable pageable);
    
    Page<Invoice> findBySubscriptionIdOrderByInvoiceDateDesc(UUID subscriptionId, Pageable pageable);
    
    @Query("SELECT i FROM Invoice i WHERE i.userId = :userId AND i.status = :status")
    List<Invoice> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);
    
    @Query("SELECT i FROM Invoice i WHERE i.status = 'open' AND i.dueDate <= :date")
    List<Invoice> findOverdueInvoices(@Param("date") LocalDateTime date);
    
    @Query("SELECT i FROM Invoice i WHERE i.userId = :userId AND i.invoiceDate >= :startDate " +
           "AND i.invoiceDate <= :endDate")
    List<Invoice> findByUserIdAndDateRange(@Param("userId") UUID userId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    
    @Modifying
    @Query("UPDATE Invoice i SET i.status = :status, i.paidAt = :paidAt WHERE i.id = :id")
    void updateInvoiceStatus(@Param("id") UUID id, @Param("status") String status, 
                            @Param("paidAt") LocalDateTime paidAt);
    
    @Query("SELECT MAX(CAST(SUBSTRING(i.invoiceNumber, 5) AS integer)) FROM Invoice i " +
           "WHERE i.invoiceNumber LIKE :prefix%")
    Integer getMaxInvoiceNumber(@Param("prefix") String prefix);
}

// InvoiceLineItemRepository.java
package com.virtualcompanion.billingservice.repository;

import com.virtualcompanion.billingservice.entity.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {
    
    List<InvoiceLineItem> findByInvoiceIdOrderByCreatedAt(UUID invoiceId);
    
    void deleteByInvoiceId(UUID invoiceId);
}

// UsageRecordRepository.java
package com.virtualcompanion.billingservice.repository;

import com.virtualcompanion.billingservice.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {
    
    @Query("SELECT ur FROM UsageRecord ur WHERE ur.userId = :userId AND ur.usageType = :type " +
           "AND ur.timestamp >= :startDate AND ur.timestamp <= :endDate")
    List<UsageRecord> findByUserIdAndTypeInPeriod(@Param("userId") UUID userId,
                                                   @Param("type") String type,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT ur.usageType, SUM(ur.quantity) FROM UsageRecord ur " +
           "WHERE ur.userId = :userId AND ur.timestamp >= :startDate AND ur.timestamp <= :endDate " +
           "GROUP BY ur.usageType")
    List<Object[]> getUsageSummaryByUser(@Param("userId") UUID userId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(ur.quantity) FROM UsageRecord ur WHERE ur.userId = :userId " +
           "AND ur.usageType = :type AND ur.timestamp >= :startDate AND ur.timestamp <= :endDate")
    Double getTotalUsageByTypeInPeriod(@Param("userId") UUID userId,
                                       @Param("type") String type,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT ur FROM UsageRecord ur WHERE ur.isBilled = false AND ur.timestamp <= :cutoffDate")
    List<UsageRecord> findUnbilledRecords(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    void deleteByTimestampBefore(LocalDateTime timestamp);
}

// BillingEventRepository.java
package com.virtualcompanion.billingservice.repository;

import com.virtualcompanion.billingservice.entity.BillingEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BillingEventRepository extends JpaRepository<BillingEvent, UUID> {
    
    Page<BillingEvent> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    @Query("SELECT be FROM BillingEvent be WHERE be.userId = :userId AND be.eventType = :eventType " +
           "AND be.createdAt >= :startDate")
    List<BillingEvent> findByUserIdAndTypeAfter(@Param("userId") UUID userId,
                                                @Param("eventType") String eventType,
                                                @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT be FROM BillingEvent be WHERE be.resourceId = :resourceId " +
           "AND be.resourceType = :resourceType ORDER BY be.createdAt DESC")
    List<BillingEvent> findByResource(@Param("resourceId") UUID resourceId,
                                     @Param("resourceType") String resourceType);
    
    void deleteByCreatedAtBefore(LocalDateTime timestamp);
}