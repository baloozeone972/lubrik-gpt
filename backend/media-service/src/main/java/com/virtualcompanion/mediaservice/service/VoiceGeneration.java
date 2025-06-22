package com.virtualcompanion.mediaservice.service;

// Generate voice asynchronously
UUID generationId = generation.getId(); =VoiceGeneration.

builder()
                .

userId(userId)
                .

characterId(request.getCharacterId())
        .

conversationId(request.getConversationId())
        .

textContent(request.getText())
        .

textHash(textHash)
                .

provider(request.getProvider() !=null?request.

getProvider() :voiceConfig.

getProvider())
        .

voiceId(request.getVoiceId() !=null?request.

getVoiceId() :voiceConfig.

getVoiceId())
        .

voiceSettings(convertSettings(request.getSettings()))
        .

outputFormat(request.getOutputFormat() !=null?request.

getOutputFormat() :"mp3")
        .

status("processing")
                .

build();

generation =voiceGenerationRepository.

save(generation);

processVoiceGeneration(generationId, request, voiceConfig);

builder()
        
        return VoiceGenerationResponse.

id(generationId)
                .

status("processing")
                .

createdAt(generation.getCreatedAt()
                .

build();)
        .

@Override
public VoiceGenerationResponse getVoiceGeneration(UUID userId, UUID generationId) {
    VoiceGeneration generation = voiceGenerationRepository.findById(generationId)
            .filter(g -> g.getUserId().equals(userId))
            .orElseThrow(() -> new VoiceGenerationException("Voice generation not found"));

    return buildResponse(generation);
}
    }

record
VoiceGeneration generation
