package com.virtualcompanion.userservice.service;

public class TwoFactorServiceImpl implements TwoFactorService {
    
    private final GoogleAuthenticator googleAuthenticator;
    
    @Value("${app.name}")
    private String appName;
    
    public TwoFactorServiceImpl() {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setCodeDigits(6)
                .setTimeStepSizeInMillis(30000)
                .setWindowSize(3)
                .build();
                
        this.googleAuthenticator = new GoogleAuthenticator(config);
    }
    
    @Override
    public String generateSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }
    
    @Override
    public String generateQrCodeUrl(String email, String secret) {
        String otpAuthUrl = String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            appName,
            email,
            secret,
            appName
        );
        
        try {
            return generateQrCodeDataUrl(otpAuthUrl);
        } catch (Exception e) {
            log.error("Failed to generate QR code for user {}: {}", email, e.getMessage());
            return otpAuthUrl; // Return the URL as fallback
        }
    }
    
    @Override
    public boolean verifyCode(String secret, String code) {
        try {
            int codeInt = Integer.parseInt(code);
            return googleAuthenticator.authorize(secret, codeInt);
        } catch (NumberFormatException e) {
            log.error("Invalid code format: {}", code);
            return false;
        }
    }
    
    private String generateQrCodeDataUrl(String text) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        byte[] qrCodeBytes = outputStream.toByteArray();
        String base64 = Base64.getEncoder().encodeToString(qrCodeBytes);
        
        return "data:image/png;base64," + base64;
    }
}
