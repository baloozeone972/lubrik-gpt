package com.virtualcompanion.conversationservice.websocket;

public class ConversationWebSocketHandler implements WebSocketHandler {

    private final MessageService messageService;
    private final AIProcessorService aiProcessor;
    private final ConversationService conversationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    
    // Sessions actives par utilisateur
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    // Flux de messages par conversation
    private final Map<String, Sinks.Many<WebSocketMessage>> conversationSinks = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        log.info("WebSocket connection established: {}", sessionId);

        // Authentifier l'utilisateur
        return authenticateSession(session)
                .flatMap(userId -> {
                    activeSessions.put(userId, session);
                    
                    // Gérer les messages entrants
                    Mono<Void> input = session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .flatMap(message -> handleIncomingMessage(userId, message, session))
                            .doOnError(error -> log.error("Error handling message: ", error))
                            .then();
                    
                    // Envoyer le ping/pong pour maintenir la connexion
                    Flux<WebSocketMessage> pingPong = Flux.interval(Duration.ofSeconds(30))
                            .map(tick -> session.pingMessage(factory -> 
                                    factory.wrap("ping".getBytes())));
                    
                    // Combiner les flux sortants
                    Mono<Void> output = session.send(
                            Flux.merge(
                                    getOutgoingMessages(userId, session),
                                    pingPong
                            )
                    );
                    
                    return Mono.zip(input, output).then();
                })
                .doFinally(signal -> {
                    log.info("WebSocket connection closed: {} - {}", sessionId, signal);
                    removeSession(sessionId);
                });
    }

    // ========== Authentication ==========

    private Mono<String> authenticateSession(WebSocketSession session) {
        String token = extractToken(session);
        if (token == null) {
            return Mono.error(new UnauthorizedException("No token provided"));
        }
        
        return Mono.fromCallable(() -> {
            if (jwtTokenProvider.validateToken(token)) {
                return jwtTokenProvider.getUserIdFromToken(token);
            }
            throw new UnauthorizedException("Invalid token");
        });
    }

    private String extractToken(WebSocketSession session) {
        // Extraire le token depuis les query params ou headers
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];
        }
        return null;
    }

    // ========== Message Handling ==========

    private Mono<Void> handleIncomingMessage(String userId, String message, WebSocketSession session) {
        return Mono.fromCallable(() -> objectMapper.readValue(message, WebSocketMessageDto.class))
                .flatMap(wsMessage -> {
                    log.debug("Received message type: {} from user: {}", wsMessage.getType(), userId);
                    
                    return switch (wsMessage.getType()) {
                        case MESSAGE -> handleChatMessage(userId, wsMessage, session);
                        case TYPING -> handleTypingIndicator(userId, wsMessage);
                        case READ_RECEIPT -> handleReadReceipt(userId, wsMessage);
                        case JOIN_CONVERSATION -> handleJoinConversation(userId, wsMessage, session);
                        case LEAVE_CONVERSATION -> handleLeaveConversation(userId, wsMessage);
                        case VOICE_START -> handleVoiceStart(userId, wsMessage);
                        case VOICE_END -> handleVoiceEnd(userId, wsMessage);
                        default -> Mono.empty();
                    };
                })
                .onErrorResume(error -> {
                    log.error("Error processing message: ", error);
                    return sendError(session, error.getMessage());
                });
    }

    // ========== Chat Messages ==========

    private Mono<Void> handleChatMessage(String userId, WebSocketMessageDto wsMessage, WebSocketSession session) {
        ChatMessagePayload payload = objectMapper.convertValue(wsMessage.getPayload(), ChatMessagePayload.class);
        
        MessageCreateDto messageDto = MessageCreateDto.builder()
                .content(payload.getContent())
                .attachments(payload.getAttachments())
                .build();
        
        return messageService.sendMessage(payload.getConversationId(), messageDto, userId)
                .flatMap(userMessage -> {
                    // Envoyer la confirmation à l'utilisateur
                    sendMessageToUser(userId, WebSocketMessageDto.builder()
                            .type(MessageType.MESSAGE_SENT)
                            .payload(userMessage)
                            .timestamp(LocalDateTime.now())
                            .build());
                    
                    // Générer et streamer la réponse IA
                    return streamAIResponse(payload.getConversationId(), userMessage, session);
                });
    }

    private Mono<Void> streamAIResponse(String conversationId, MessageDto userMessage, WebSocketSession session) {
        return aiProcessor.streamResponse(conversationId, userMessage)
                .doOnNext(chunk -> {
                    WebSocketMessageDto wsMessage = WebSocketMessageDto.builder()
                            .type(MessageType.AI_RESPONSE_CHUNK)
                            .payload(AIResponseChunk.builder()
                                    .conversationId(conversationId)
                                    .chunk(chunk.getContent())
                                    .isComplete(chunk.isComplete())
                                    .build())
                            .timestamp(LocalDateTime.now())
                            .build();
                    
                    sendMessageToSession(session, wsMessage);
                })
                .then();
    }

    // ========== Typing Indicators ==========

    private Mono<Void> handleTypingIndicator(String userId, WebSocketMessageDto wsMessage) {
        TypingPayload payload = objectMapper.convertValue(wsMessage.getPayload(), TypingPayload.class);
        
        // Broadcaster l'indicateur aux autres participants
        return broadcastToConversation(payload.getConversationId(), WebSocketMessageDto.builder()
                .type(MessageType.USER_TYPING)
                .payload(UserTypingPayload.builder()
                        .userId(userId)
                        .conversationId(payload.getConversationId())
                        .isTyping(payload.isTyping())
                        .build())
                .timestamp(LocalDateTime.now())
                .build(), userId); // Exclure l'expéditeur
    }

    // ========== Conversation Management ==========

    private Mono<Void> handleJoinConversation(String userId, WebSocketMessageDto wsMessage, WebSocketSession session) {
        JoinConversationPayload payload = objectMapper.convertValue(
                wsMessage.getPayload(), JoinConversationPayload.class);
        
        String conversationId = payload.getConversationId();
        
        // Créer un sink pour cette conversation si nécessaire
        Sinks.Many<WebSocketMessage> sink = conversationSinks.computeIfAbsent(
                conversationId,
                k -> Sinks.many().multicast().onBackpressureBuffer()
        );
        
        // Charger les messages récents
        return messageService.getRecentMessages(conversationId, userId, 50)
                .collectList()
                .map(messages -> WebSocketMessageDto.builder()
                        .type(MessageType.CONVERSATION_HISTORY)
                        .payload(ConversationHistoryPayload.builder()
                                .conversationId(conversationId)
                                .messages(messages)
                                .build())
                        .timestamp(LocalDateTime.now())
                        .build())
                .doOnNext(wsMessage -> sendMessageToSession(session, wsMessage))
                .then();
    }

    private Mono<Void> handleLeaveConversation(String userId, WebSocketMessageDto wsMessage) {
        LeaveConversationPayload payload = objectMapper.convertValue(
                wsMessage.getPayload(), LeaveConversationPayload.class);
        
        // Nettoyer les ressources
        return Mono.fromRunnable(() -> {
            log.info("User {} leaving conversation {}", userId, payload.getConversationId());
            // Logique de nettoyage si nécessaire
        });
    }

    // ========== Voice Chat ==========

    private Mono<Void> handleVoiceStart(String userId, WebSocketMessageDto wsMessage) {
        VoiceStartPayload payload = objectMapper.convertValue(wsMessage.getPayload(), VoiceStartPayload.class);
        
        return conversationService.startVoiceSession(payload.getConversationId(), userId)
                .map(session -> WebSocketMessageDto.builder()
                        .type(MessageType.VOICE_SESSION_STARTED)
                        .payload(VoiceSessionPayload.builder()
                                .sessionId(session.getId())
                                .iceServers(session.getIceServers())
                                .build())
                        .timestamp(LocalDateTime.now())
                        .build())
                .doOnNext(response -> sendMessageToUser(userId, response))
                .then();
    }

    private Mono<Void> handleVoiceEnd(String userId, WebSocketMessageDto wsMessage) {
        VoiceEndPayload payload = objectMapper.convertValue(wsMessage.getPayload(), VoiceEndPayload.class);
        
        return conversationService.endVoiceSession(payload.getSessionId(), userId)
                .then(Mono.fromRunnable(() -> 
                        sendMessageToUser(userId, WebSocketMessageDto.builder()
                                .type(MessageType.VOICE_SESSION_ENDED)
                                .timestamp(LocalDateTime.now())
                                .build())
                ));
    }

    // ========== Read Receipts ==========

    private Mono<Void> handleReadReceipt(String userId, WebSocketMessageDto wsMessage) {
        ReadReceiptPayload payload = objectMapper.convertValue(wsMessage.getPayload(), ReadReceiptPayload.class);
        
        return messageService.markAsRead(payload.getMessageIds(), userId)
                .then(broadcastToConversation(
                        payload.getConversationId(),
                        WebSocketMessageDto.builder()
                                .type(MessageType.MESSAGES_READ)
                                .payload(MessagesReadPayload.builder()
                                        .userId(userId)
                                        .messageIds(payload.getMessageIds())
                                        .readAt(LocalDateTime.now())
                                        .build())
                                .timestamp(LocalDateTime.now())
                                .build(),
                        userId
                ));
    }

    // ========== Outgoing Messages ==========

    private Flux<WebSocketMessage> getOutgoingMessages(String userId, WebSocketSession session) {
        // Créer un flux personnel pour cet utilisateur
        Sinks.Many<WebSocketMessage> userSink = Sinks.many().multicast().onBackpressureBuffer();
        
        return userSink.asFlux()
                .doOnError(error -> log.error("Error in outgoing message flux: ", error))
                .onErrorContinue((error, obj) -> 
                        log.error("Error sending message to user {}: ", userId, error));
    }

    // ========== Utility Methods ==========

    private void sendMessageToUser(String userId, WebSocketMessageDto message) {
        WebSocketSession session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            sendMessageToSession(session, message);
        }
    }

    private void sendMessageToSession(WebSocketSession session, WebSocketMessageDto message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.send(Mono.just(session.textMessage(json))).subscribe();
        } catch (Exception e) {
            log.error("Error sending message to session: ", e);
        }
    }

    private Mono<Void> broadcastToConversation(String conversationId, WebSocketMessageDto message, String excludeUserId) {
        return conversationService.getConversationParticipants(conversationId)
                .filter(userId -> !userId.equals(excludeUserId))
                .doOnNext(userId -> sendMessageToUser(userId, message))
                .then();
    }

    private Mono<Void> sendError(WebSocketSession session, String error) {
        WebSocketMessageDto errorMessage = WebSocketMessageDto.builder()
                .type(MessageType.ERROR)
                .payload(ErrorPayload.builder()
                        .error(error)
                        .timestamp(LocalDateTime.now())
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
        
        return session.send(Mono.just(session.textMessage(toJson(errorMessage))));
    }

    private void removeSession(String sessionId) {
        activeSessions.entrySet().removeIf(entry -> 
                entry.getValue().getId().equals(sessionId));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error converting to JSON: ", e);
            return "{}";
        }
    }
}
