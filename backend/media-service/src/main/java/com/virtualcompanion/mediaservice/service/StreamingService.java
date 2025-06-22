package com.virtualcompanion.mediaservice.service;

public interface StreamingService {
    StreamingResponse startSession(UUID userId, StreamingRequest request);

    void setSdpAnswer(UUID userId, String sessionId, String sdpAnswer);

    void addIceCandidate(UUID userId, String sessionId, IceCandidate candidate);

    void endSession(UUID userId, String sessionId);

    StreamingSessionResponse getSession(UUID userId, String sessionId);
}
