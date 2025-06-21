package com.virtualcompanion.moderationservice.repository;

public interface ModerationMetricsRepository extends JpaRepository<ModerationMetrics, UUID> {
    
    Optional<ModerationMetrics> findByMetricDate(LocalDate date);
    
    List<ModerationMetrics> findByMetricDateBetweenOrderByMetricDate(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(mm.totalContentReviewed), SUM(mm.automatedDecisions), SUM(mm.humanDecisions) " +
           "FROM ModerationMetrics mm WHERE mm.metricDate BETWEEN :startDate AND :endDate")
    Object[] getAggregatedMetrics(@Param("startDate") LocalDate startDate, 
                                 @Param("endDate") LocalDate endDate);
}
