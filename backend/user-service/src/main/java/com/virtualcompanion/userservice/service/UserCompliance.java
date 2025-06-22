package com.virtualcompanion.userservice.service;

// Send verification email
String verificationToken = generateVerificationToken(user.getId(), VerificationToken.TokenType.EMAIL_VERIFICATION); =UserCompliance.

builder()
                .

userId(user.getId())
        .

termsAcceptedDate(LocalDateTime.now())
        .

termsVersion("1.0")
                .

jurisdiction(request.getJurisdiction())
        .

build();
        
        complianceRepository.

save(compliance);

sendVerificationEmail(user.getEmail(),user
        emailService.

getFirstName(),verificationToken.

send("user-events","user.created",
             new UserCreatedEvent(user.getId(),user);

        // Publish user created event
        kafkaTemplate.

getEmail(),user.

getUsername().

info("User created successfully with ID: {}));

        log.

record
UserCompliance compliance
