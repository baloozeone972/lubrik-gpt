// ConversationController.java
package com.virtualcompanion.conversationservice.controller;

import com.virtualcompanion.conversationservice.dto.*;
import com.virtualcompanion.conversationservice.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Conversation management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class ConversationController {
    
    private final ConversationService conversationService;
    
    @PostMapping
    @Operation(summary = "Start a new conversation")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<ConversationResponse>> startConversation(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody StartConversationRequest request) {
        return conversationService.startConversation(userId, request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
    
    @GetMapping("/{conversationId}")
    @Operation(summary = "Get conversation details")
    public Mono<ResponseEntity<ConversationResponse>> getConversation(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID conversationId) {
        return conversationService.getConversation(userId, conversationId)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Send a message")
    public Mono<ResponseEntity<MessageResponse>> sendMessage(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return conversationService.sendMessage(userId, conversationId, request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping(value = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream a message response")
    public Flux<ServerSentEvent<StreamingMessageEvent>> streamMessage(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return conversationService.streamMessage(userId, conversationId, request)
                .map(event -> ServerSentEvent.<StreamingMessageEvent>builder()
                        .event(event.getEventType())
                        .data(event)
                        .build())
                .doOnError(error -> ServerSentEvent.<StreamingMessageEvent>builder()
                        .event("error")
                        .data(StreamingMessageEvent.builder()
                                .eventType("error")
                                .content(error.getMessage())
                                .build())
                        .build());
    }
    
    @PostMapping("/{conversationId}/end")
    @Operation(summary = "End a conversation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> endConversation(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID conversationId) {
        return conversationService.endConversation(userId, conversationId)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
    
    @GetMapping
    @Operation(summary = "Get user's conversations")
    public Mono<ResponseEntity<Page<ConversationResponse>>> getUserConversations(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastActivityAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return conversationService.getUserConversations(userId, pageRequest)
                .map(ResponseEntity::ok);
    }
    
    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get conversation messages")
    public Mono<ResponseEntity<Page<MessageResponse>>> getConversationMessages(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return conversationService.getConversationMessages(userId, conversationId, pageRequest)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/history")
    @Operation(summary = "Get conversation history with filters")
    public Mono<ResponseEntity<ConversationHistoryResponse>> getConversationHistory(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ConversationHistoryRequest request) {
        return conversationService.getConversationHistory(userId, request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/export")
    @Operation(summary = "Export conversations")
    public Mono<ResponseEntity<ConversationExportResponse>> exportConversations(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ConversationExportRequest request) {
        return conversationService.exportConversations(userId, request)
                .map(ResponseEntity::ok);
    }
    
    @PostMapping("/memory/update")
    @Operation(summary = "Update character memory")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> updateMemory(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody MemoryUpdateRequest request) {
        return conversationService.updateMemory(userId, request)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
    
    @GetMapping("/statistics/{characterId}")
    @Operation(summary = "Get conversation statistics for a character")
    public Mono<ResponseEntity<ConversationStatistics>> getStatistics(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID characterId) {
        return conversationService.getConversationStatistics(userId, characterId)
                .map(ResponseEntity::ok);
    }
}

// WebSocketController.java
package com.virtualcompanion.conversationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualcompanion.conversationservice.dto.*;
import com.virtualcompanion.conversationservice.service.ConversationService;
import com.virtualcompanion.conversationservice.service.WebSocketSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
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