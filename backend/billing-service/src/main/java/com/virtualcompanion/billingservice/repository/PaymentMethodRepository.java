package com.virtualcompanion.billingservice.repository;

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
