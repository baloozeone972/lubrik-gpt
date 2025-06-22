package com.virtualcompanion.billingservice.repository;

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
