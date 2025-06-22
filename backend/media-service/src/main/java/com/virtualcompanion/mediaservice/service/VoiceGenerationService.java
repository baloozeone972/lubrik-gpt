package com.virtualcompanion.mediaservice.service;

public interface VoiceGenerationService {
    VoiceGenerationResponse generateVoice(UUID userId, VoiceGenerationRequest request);

    VoiceGenerationResponse getVoiceGeneration(UUID userId, UUID generationId);

    Page<VoiceGenerationResponse> getUserVoiceGenerations(UUID userId, Pageable pageable);

    byte[] getAudioContent(UUID generationId);

    void deleteVoiceGeneration(UUID userId, UUID generationId);
}
