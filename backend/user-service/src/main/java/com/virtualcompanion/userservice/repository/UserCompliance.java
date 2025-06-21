package com.virtualcompanion.userservice.repository;

record
        UserCompliance compliance = UserCompliance.builder()
                .user(user)
                .termsAcceptedVersion("1.0")
                .termsAcceptedAt(LocalDateTime.now())
                .privacyAcceptedVersion("1.0")
                .privacyAcceptedAt(LocalDateTime.now())
                .marketingConsent(request.getMarketingConsent() != null && request.getMarketingConsent())
                .dataProcessingConsent(true)
                .build();
        
        user.setCompliance(compliance);
        user = userRepository.save(user);
        
        // Send verification email
        createAndSendVerificationToken(user, TokenType.EMAIL_VERIFICATION);
        
        // Publish event
        publishUserEvent("USER_REGISTERED", user.getId(), null);
        
        log.info("User registered successfully with ID: {}
