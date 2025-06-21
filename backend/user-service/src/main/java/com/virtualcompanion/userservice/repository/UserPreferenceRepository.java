package com.virtualcompanion.userservice.repository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {
    
    List<UserPreference> findByUserId(UUID userId);
    
    Optional<UserPreference> findByUserIdAndKey(UUID userId, String key);
    
    void deleteByUserIdAndKey(UUID userId, String key);
}
