package com.virtualcompanion.billingservice.repository;

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
