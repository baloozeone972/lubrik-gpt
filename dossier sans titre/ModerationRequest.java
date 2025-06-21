package com.virtualcompanion.moderationservice.service.impl;

record
            ModerationRequest moderationRequest = ModerationRequest.builder()
                    .contentType("image")
                    .contentId(request.getImageUrl())
                    .userId(request.getUserId())
                    .characterId(request.getCharacterId())
                    .status("pending")
                    .priority(determinePriority(request))
                    .metadata(request.getMetadata())
                    .build();
            
            moderationRequest = requestRepository.save(moderationRequest);
            
            // Get user details
            UserDetails userDetails = userServiceClient.getUser(request.getUserId());
            String jurisdiction = userDetails.getJurisdiction();
            Integer userAge = userDetails.getVerifiedAge();
            
            // Apply rules
            List<ModerationRule> rules = ruleEngine.getApplicableRules("image", jurisdiction, userAge);
            
            // Run through moderation providers
            List<ModerationProvider> providers = providerFactory.getProvidersForType("image");
            Map<String, ModerationProvider.ImageResult> providerResults = new HashMap<>();
            
            for (ModerationProvider provider : providers) {
                try {
                    ModerationProvider.ImageResult result = provider.moderateImage(request.getImageUrl());
                    providerResults.put(provider.getName(), result);
                } catch (Exception e) {
                    log.error("Provider {} failed: {}", provider.getName(), e.getMessage());
                }
            }
