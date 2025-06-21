package com.virtualcompanion.mediaservice.service;

public class KurentoStreamingService implements StreamingService {
    
    private final KurentoClient kurentoClient;
    private final StreamingSessionRepository sessionRepository;
    private final StreamingMapper streamingMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private final Map<String, MediaPipeline> pipelines = new ConcurrentHashMap<>();
    private final Map<String, WebRtcEndpoint> endpoints = new ConcurrentHashMap<>();
    
    @Value("${webrtc.max-bandwidth.video}")
    private int maxVideoBandwidth;
    
    @Value("${webrtc.max-bandwidth.audio}")
    private int maxAudioBandwidth;
    
    @Override
    public StreamingResponse startSession(UUID userId, StreamingRequest request) {
        log.info("Starting streaming session for user: {} character: {}", userId, request.getCharacterId());
        
        try {
            // Check concurrent sessions limit
            long activeSessions = sessionRepository.countActiveSessionsByUser(userId);
            if (activeSessions >= 3) {
                throw new StreamingException("Maximum concurrent sessions reached");
            }
            
            // Create Kurento media pipeline
            MediaPipeline pipeline = kurentoClient.createMediaPipeline();
            
            // Create WebRTC endpoint
            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            
            // Configure bandwidth limits
            webRtcEndpoint.setMaxVideoSendBandwidth(maxVideoBandwidth);
            webRtcEndpoint.setMaxAudioSendBandwidth(maxAudioBandwidth);
            
            // Add ICE candidate listener
            List<IceCandidate> candidates = new ArrayList<>();
            webRtcEndpoint.addIceCandidateFoundListener(event -> {
                IceCandidate candidate = streamingMapper.toDto(event.getCandidate());
                candidates.add(candidate);
                
                // Send candidate to client via WebSocket
                publishIceCandidate(userId.toString(), candidate);
            });
            
            // Configure recording if enabled
            if (request.getConfig() != null && Boolean.TRUE.equals(request.getConfig().getEnableRecording())) {
                configureRecording(pipeline, webRtcEndpoint, userId, request);
            }
            
            // Generate offer
            String sdpOffer = webRtcEndpoint.generateOffer();
            
            // Create session
            String sessionId = UUID.randomUUID().toString();
            StreamingSession session = StreamingSession.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .characterId(request.getCharacterId())
                    .conversationId(request.getConversationId())
                    .sessionType(request.getStreamType())
                    .kurentoSessionId(pipeline.getId())
                    .sdpOffer(sdpOffer)
                    .iceCandiates(JsonUtils.toJson(candidates))
                    .status("waiting_answer")
                    .metadata(request.getMetadata())
                    .build();
            
            session = sessionRepository.save(session);
            
            // Store references
            pipelines.put(sessionId, pipeline);
            endpoints.put(sessionId, webRtcEndpoint);
            
            // Gather ICE candidates
            webRtcEndpoint.gatherCandidates();
            
            // Publish event
            publishStreamingEvent(session, "session_started");
            
            return StreamingResponse.builder()
                    .sessionId(sessionId)
                    .kurentoSessionId(pipeline.getId())
                    .sdpOffer(sdpOffer)
                    .iceCandidates(candidates)
                    .status("waiting_answer")
                    .startedAt(session.getStartedAt())
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to start streaming session: {}", e.getMessage());
            throw new StreamingException("Failed to start streaming session: " + e.getMessage());
        }
    }
    
    @Override
    public void setSdpAnswer(UUID userId, String sessionId, String sdpAnswer) {
        StreamingSession session = sessionRepository.findBySessionId(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new StreamingException("Session not found"));
        
        WebRtcEndpoint endpoint = endpoints.get(sessionId);
        if (endpoint == null) {
            throw new StreamingException("WebRTC endpoint not found");
        }
        
        try {
            // Process SDP answer
            endpoint.processAnswer(sdpAnswer);
            
            // Update session
            sessionRepository.updateSdpAnswer(sessionId, sdpAnswer);
            session.setStatus("connected");
            sessionRepository.save(session);
            
            // Publish event
            publishStreamingEvent(session, "session_connected");
            
            log.info("SDP answer processed for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Failed to process SDP answer: {}", e.getMessage());
            throw new StreamingException("Failed to process SDP answer: " + e.getMessage());
        }
    }
    
    @Override
    public void addIceCandidate(UUID userId, String sessionId, IceCandidate candidate) {
        StreamingSession session = sessionRepository.findBySessionId(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new StreamingException("Session not found"));
        
        WebRtcEndpoint endpoint = endpoints.get(sessionId);
        if (endpoint == null) {
            throw new StreamingException("WebRTC endpoint not found");
        }
        
        try {
            // Add ICE candidate
            org.kurento.client.IceCandidate kurentoCandidate = new org.kurento.client.IceCandidate(
                    candidate.getCandidate(),
                    candidate.getSdpMid(),
                    Integer.parseInt(candidate.getSdpMLineIndex())
            );
            
            endpoint.addIceCandidate(kurentoCandidate);
            
            log.debug("ICE candidate added for session: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Failed to add ICE candidate: {}", e.getMessage());
        }
    }
    
    @Override
    public void endSession(UUID userId, String sessionId) {
        StreamingSession session = sessionRepository.findBySessionId(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new StreamingException("Session not found"));
        
        try {
            // Clean up Kurento resources
            WebRtcEndpoint endpoint = endpoints.remove(sessionId);
            if (endpoint != null) {
                endpoint.release();
            }
            
            MediaPipeline pipeline = pipelines.remove(sessionId);
            if (pipeline != null) {
                pipeline.release();
            }
            
            // Update session
            sessionRepository.endSession(sessionId, "ended", LocalDateTime.now());
            
            // Publish event
            publishStreamingEvent(session, "session_ended");
            
            log.info("Streaming session ended: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Failed to end session: {}", e.getMessage());
        }
    }
    
    @Override
    public StreamingSessionResponse getSession(UUID userId, String sessionId) {
        StreamingSession session = sessionRepository.findBySessionId(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new StreamingException("Session not found"));
        
        return streamingMapper.toResponse(session);
    }
    
    private void configureRecording(MediaPipeline pipeline, WebRtcEndpoint endpoint, UUID userId, StreamingRequest request) {
        try {
            // Create recorder endpoint
            String recordingPath = String.format("/recordings/%s/%s_%s.webm",
                    userId,
                    request.getCharacterId(),
                    System.currentTimeMillis()
            );
            
            RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline, "file://" + recordingPath).build();
            
            // Connect WebRTC endpoint to recorder
            endpoint.connect(recorder);
            
            // Start recording
            recorder.record();
            
            log.info("Recording started for session");
            
        } catch (Exception e) {
            log.error("Failed to configure recording: {}", e.getMessage());
        }
    }
    
    private void publishStreamingEvent(StreamingSession session, String eventType) {
        Map<String, Object> event = Map.of(
                "sessionId", session.getSessionId(),
                "userId", session.getUserId(),
                "characterId", session.getCharacterId(),
                "eventType", eventType,
                "timestamp", LocalDateTime.now()
        );
        
        kafkaTemplate.send("streaming-events", "streaming." + eventType, event);
    }
    
    private void publishIceCandidate(String userId, IceCandidate candidate) {
        Map<String, Object> event = Map.of(
                "userId", userId,
                "type", "ice-candidate",
                "candidate", candidate,
                "timestamp", LocalDateTime.now()
        );
        
        kafkaTemplate.send("websocket-events", "ice.candidate", event);
    }
}
