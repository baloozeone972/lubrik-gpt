package com.virtualcompanion.userservice.dto.request;

public class TwoFactorSetupResponse {
    private String secret;
    private String qrCodeUrl;
    private List<String> backupCodes;
}
