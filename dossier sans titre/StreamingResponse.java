package com.virtualcompanion.mediaservice.dto;

public class StreamingResponse {
    
    private String sessionId;
    private String kurentoSessionId;
    private String sdpOffer;
    private List<IceCandidate> iceCandidates;
    private String status;
    private LocalDateTime startedAt;
}
