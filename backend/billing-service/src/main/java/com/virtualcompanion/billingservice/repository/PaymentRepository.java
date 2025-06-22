package com.virtualcompanion.billingservice.repository;

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
