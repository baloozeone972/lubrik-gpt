package com.virtualcompanion.moderationservice.repository;

public interface ModerationRuleRepository extends JpaRepository<ModerationRule, UUID> {
    
    List<ModerationRule> findByContentTypeAndIsActiveTrue(String contentType);
    
    List<ModerationRule> findByCategoryAndIsActiveTrue(String category);
    
    @Query("SELECT mr FROM ModerationRule mr WHERE mr.jurisdiction = :jurisdiction " +
           "AND mr.isActive = true ORDER BY mr.priority DESC")
    List<ModerationRule> findActiveRulesByJurisdiction(@Param("jurisdiction") String jurisdiction);
    
    @Query("SELECT mr FROM ModerationRule mr WHERE mr.ruleType = :type AND mr.isActive = true")
    List<ModerationRule> findActiveRulesByType(@Param("type") String type);
    
    boolean existsByNameAndIsActiveTrue(String name);
}
