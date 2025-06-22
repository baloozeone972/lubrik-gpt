package com.virtualcompanion.mediaservice.controller;

public class StreamingController {

    private final StreamingService streamingService;

    @PostMapping("/sessions")
    @Operation(summary = "Start streaming session")
    public ResponseEntity<StreamingResponse> startSession(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody StreamingRequest request) {

        StreamingResponse response = streamingService.startSession(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/answer")
    @Operation(summary = "Set SDP answer")
    public ResponseEntity<Void> setSdpAnswer(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String sessionId,
            @RequestBody String sdpAnswer) {

        streamingService.setSdpAnswer(userId, sessionId, sdpAnswer);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{sessionId}/ice-candidate")
    @Operation(summary = "Add ICE candidate")
    public ResponseEntity<Void> addIceCandidate(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String sessionId,
            @RequestBody IceCandidate candidate) {

        streamingService.addIceCandidate(userId, sessionId, candidate);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{sessionId}/end")
    @Operation(summary = "End streaming session")
    public ResponseEntity<Void> endSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String sessionId) {

        streamingService.endSession(userId, sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get session details")
    public ResponseEntity<StreamingSessionResponse> getSession(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String sessionId) {

        StreamingSessionResponse response = streamingService.getSession(userId, sessionId);
        return ResponseEntity.ok(response);
    }
}
