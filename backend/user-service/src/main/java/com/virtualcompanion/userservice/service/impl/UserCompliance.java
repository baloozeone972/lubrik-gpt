package com.virtualcompanion.userservice.service.impl;

record
        UserCompliance compliance = UserCompliance.builder()
                .userId(user.getId())
                .termsAcceptedDate(LocalDateTime.now())
                .termsVersion("1.0")
                .jurisdiction(request.getJurisdiction())
                .build();
        
        complianceRepository.save(compliance);
        
        // Send verification email
        String verificationToken = generateVerificationToken(user.getId(), VerificationToken.TokenType.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verificationToken);
        
        // Publish user created event
        kafkaTemplate.send("user-events", "user.created", 
            new UserCreatedEvent(user.getId(), user.getEmail(), user.getUsername()));
        
        log.info("User created successfully with ID: {}
