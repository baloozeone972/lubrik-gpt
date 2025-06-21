package com.virtualcompanion.billingservice.repository;

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
