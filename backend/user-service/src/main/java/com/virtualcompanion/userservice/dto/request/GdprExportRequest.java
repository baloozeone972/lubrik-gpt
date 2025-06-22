package com.virtualcompanion.userservice.dto.request;

public class GdprExportRequest {

    @NotBlank(message = "Password is required for data export")
    private String password;

    private ExportFormat format = ExportFormat.JSON;

    public enum ExportFormat {
        JSON,
        CSV,
        PDF
    }
}
