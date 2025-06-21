package com.virtualcompanion.userservice.service;

public interface TwoFactorService {
    String generateSecret();
    String generateQrCodeUrl(String email, String secret);
    boolean verifyCode(String secret, String code);
}
