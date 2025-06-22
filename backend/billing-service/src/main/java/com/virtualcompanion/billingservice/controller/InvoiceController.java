package com.virtualcompanion.billingservice.controller;

public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @Operation(summary = "Get user's invoices")
    public ResponseEntity<Page<InvoiceResponse>> getUserInvoices(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "invoiceDate"));
        Page<InvoiceResponse> invoices = invoiceService.getUserInvoices(userId, pageRequest);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{invoiceId}")
    @Operation(summary = "Get invoice details")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID invoiceId) {

        InvoiceResponse invoice = invoiceService.getInvoice(userId, invoiceId);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/{invoiceId}/download")
    @Operation(summary = "Download invoice PDF")
    public ResponseEntity<Resource> downloadInvoice(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID invoiceId) {

        byte[] pdfContent = invoiceService.generateInvoicePdf(userId, invoiceId);
        ByteArrayResource resource = new ByteArrayResource(pdfContent);

        String filename = "invoice_" + invoiceId + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentLength(pdfContent.length)
                .body(resource);
    }

    @PostMapping("/{invoiceId}/send")
    @Operation(summary = "Send invoice by email")
    public ResponseEntity<Void> sendInvoiceByEmail(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID invoiceId) {

        invoiceService.sendInvoiceByEmail(userId, invoiceId);
        return ResponseEntity.ok().build();
    }
}
