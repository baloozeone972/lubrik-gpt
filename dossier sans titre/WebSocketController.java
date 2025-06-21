package com.virtualcompanion.conversationservice.controller;

public class WebSocketController implements WebSocketHandler {
    
    private final ConversationService conversationService;
    private final WebSocketSessionService sessionService;
    private final ObjectMapper objectMapper;
    
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        UUID userId = extractUserId(session);
        
        if (userId == null) {
            return session.close();
        }
        
        // Register session
        sessionService.registerSession(sessionId, userId, session);
        
        // Handle incoming messages
        Flux<WebSocketMessage> output = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> handleMessage(sessionId, userId, payload))
                .map(response -> session.textMessage(response))
                .doOnError(error -> log.error("WebSocket error for session {}: {}", sessionId, error.getMessage()))
                .onErrorResume(error -> Flux.just(session.textMessage(createErrorResponse(error))));
        
        // Send ping messages to keep connection alive
        Flux<WebSocketMessage> pingMessages = Flux.interval(Duration.ofSeconds(30))
                .map(tick -> session.textMessage("{\"type\":\"ping\"}"));
        
        return session.send(Flux.merge(output, pingMessages))
                .doFinally(signal -> {
                    log.info("WebSocket session {} closed with signal: {}", sessionId, signal);
                    sessionService.removeSession(sessionId);
                });
    }
    
    private Flux<String> handleMessage(String sessionId, UUID userId, String payload) {
        try {
            Map<String, Object> message = objectMapper.readValue(payload, Map.class);
            String type = (String) message.get("type");
            
            switch (type) {
                case "start_conversation":
                    return handleStartConversation(userId, message);
                    
                case "send_message":
                    return handleSendMessage(userId, message);
                    
                case "stream_message":
                    return handleStreamMessage(userId, message);
                    
                case "end_conversation":
                    return handleEndConversation(userId, message);
                    
                case "pong":
                    sessionService.updateSessionActivity(sessionId);
                    return Flux.empty();
                    
                default:
                    return Flux.just(createErrorResponse("Unknown message type: " + type));
            }
        } catch (Exception e) {
            log.error("Failed to handle WebSocket message: {}", e.getMessage());
            return Flux.just(createErrorResponse(e));
        }
    }
    
    private Flux<String> handleStartConversation(UUID userId, Map<String, Object> message) {
        try {
            StartConversationRequest request = objectMapper.convertValue(
                    message.get("data"), StartConversationRequest.class);
            
            return conversationService.startConversation(userId, request)
                    .map(response -> createSuccessResponse("conversation_started", response))
                    .flux();
        } catch (Exception e) {
            return Flux.just(createErrorResponse(e));
        }
    }
    
    private Flux<String> handleSendMessage(UUID userId, Map<String, Object> message) {
        try {
            UUID conversationId = UUID.fromString((String) message.get("conversationId"));
            SendMessageRequest request = objectMapper.convertValue(
                    message.get("data"), SendMessageRequest.class);
            
            return conversationService.sendMessage(userId, conversationId, request)
                    .map(response -> createSuccessResponse("message_sent", response))
                    .flux();
        } catch (Exception e) {
            return Flux.just(createErrorResponse(e));
        }
    }
    
    private Flux<String> handleStreamMessage(UUID userId, Map<String, Object> message) {
        try {
            UUID conversationId = UUID.fromString((String) message.get("conversationId"));
            SendMessageRequest request = objectMapper.convertValue(
                    message.get("data"), SendMessageRequest.class);
            
            return conversationService.streamMessage(userId, conversationId, request)
                    .map(event -> createStreamingResponse(event))
                    .onErrorResume(error -> Flux.just(createErrorResponse(error)));
        } catch (Exception e) {
            return Flux.just(createErrorResponse(e));
        }
    }
    
    private Flux<String> handleEndConversation(UUID userId, Map<String, Object> message) {
        try {
            UUID conversationId = UUID.fromString((String) message.get("conversationId"));
            
            return conversationService.endConversation(userId, conversationId)
                    .thenReturn(createSuccessResponse("conversation_ended", Map.of("conversationId", conversationId)))
                    .flux();
        } catch (Exception e) {
            return Flux.just(createErrorResponse(e));
        }
    }
    
    private UUID extractUserId(WebSocketSession session) {
        // Extract user ID from session attributes or query parameters
        // This would typically come from JWT token validation
        String userIdStr = session.getHandshakeInfo().getUri().getQuery();
        if (userIdStr != null && userIdStr.startsWith("userId=")) {
            try {
                return UUID.fromString(userIdStr.substring(7));
            } catch (IllegalArgumentException e) {
                log.error("Invalid user ID format: {}", userIdStr);
            }
        }
        return null;
    }
    
    private String createSuccessResponse(String type, Object data) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "status", "success",
                    "data", data,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    private String createStreamingResponse(StreamingMessageEvent event) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "stream_chunk",
                    "status", "streaming",
                    "data", event,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    private String createErrorResponse(Throwable error) {
        return createErrorResponse(error.getMessage());
    }
    
    private String createErrorResponse(String errorMessage) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "error",
                    "status", "error",
                    "error", errorMessage,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return "{\"type\":\"error\",\"status\":\"error\",\"error\":\"Internal error\"}";
        }
    }
}
