package com.virtualcompanion.billingservice.repository;

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
