package com.virtualcompanion.moderationservice.service;

// Get user details
UserDetails userDetails = userServiceClient.getUser(request.getUserId()); =ModerationRequest.
String jurisdiction = userDetails.getJurisdiction();
                    .
Integer userAge = userDetails.getVerifiedAge();
                    .
// Apply rules
List<ModerationRule> rules = ruleEngine.getApplicableRules("image", jurisdiction, userAge);)
        .
// Run through moderation providers
List<ModerationProvider> providers = providerFactory.getProvidersForType("image");)
        .
Map<String, ModerationProvider.ImageResult> providerResults = new HashMap<>();)
        .
ModerationProvider provider
                    .
ModerationProvider.ImageResult result = provider.moderateImage(request.getImageUrl());)
        .
Exception e)
        .

builder()

moderationRequest =requestRepository.

contentType("image")

contentId(request.getImageUrl()
userId(request.getUserId()
characterId(request.getCharacterId()

status("pending")

priority(determinePriority(request)
metadata(request.getMetadata()
            
            for(
build(); :providers){
        try{
save(moderationRequest);
                    providerResults.

put(provider.getName(),result);
        }catch(
error("Provider {} failed: {}",provider.getName(),e){
        log.

getMessage().

record
ModerationRequest moderationRequest);
        }
        }
