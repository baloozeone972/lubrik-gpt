package com.virtualcompanion.userservice.dto.response;

public class TwoFactorSetupResponse {
    private String secret;
    private String qrCodeUrl;
    private List<String> backupCodes;
}
