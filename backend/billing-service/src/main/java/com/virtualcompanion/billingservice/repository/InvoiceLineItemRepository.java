package com.virtualcompanion.billingservice.repository;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {
    
    List<InvoiceLineItem> findByInvoiceIdOrderByCreatedAt(UUID invoiceId);
    
    void deleteByInvoiceId(UUID invoiceId);
}
