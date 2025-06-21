package com.virtualcompanion.conversationservice.service.impl;

public class ConversationServiceImpl implements ConversationService {
    
    private final ConversationRepository conversationRepository;
    private final ConversationMemoryRepository memoryRepository;
    private final StreamingSessionRepository sessionRepository;
    private final ConversationAnalyticsRepository analyticsRepository;
    private final MessageRepository messageRepository;
    private final ConversationContextRepository contextRepository;
    private final CharacterContextRepository characterContextRepository;
    
    private final AIProcessorService aiProcessor;
    private final MemoryService memoryService;
    private final AnalyticsService analyticsService;
    private final ExportService exportService;
    
    private final CharacterServiceClient characterClient;
    private final UserServiceClient userClient;
    
    private final ConversationMapper conversationMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public Mono<ConversationResponse> startConversation(UUID userId, StartConversationRequest request) {
        return Mono.fromCallable(() -> {
            // Create new conversation
            Conversation conversation = Conversation.builder()
                    .userId(userId)
                    .characterId(request.getCharacterId())
                    .status("active")
                    .mode(request.getConversationMode() != null ? request.getConversationMode() : "text")
                    .startedAt(LocalDateTime.now())
                    .lastActivityAt(LocalDateTime.now())
                    .messageCount(0L)
                    .build();
            
            conversation = conversationRepository.save(conversation);
            
            // Initialize conversation context
            ConversationContext context = ConversationContext.builder()
                    .conversationId(conversation.getId())
                    .userId(userId)
                    .characterId(request.getCharacterId())
                    .settings(request.getSettings())
                    .contextData(request.getContext() != null ? request.getContext() : new HashMap<>())
                    .isActive(true)
                    .build();
            
            contextRepository.save(context);
            
            // Load character context
            characterContextRepository.findByUserIdAndCharacterId(userId, request.getCharacterId())
                    .orElseGet(() -> {
                        var newContext = characterContextRepository.save(
                            CharacterContext.builder()
                                    .userId(userId)
                                    .characterId(request.getCharacterId())
                                    .relationshipLevel(0)
                                    .sharedMemories(new ArrayList<>())
                                    .preferences(new HashMap<>())
                                    .isActive(true)
                                    .build()
                        );
                        return newContext;
                    });
            
            // Send initial message if provided
            if (request.getInitialMessage() != null && !request.getInitialMessage().isEmpty()) {
                Message userMessage = Message.builder()
                        .conversationId(conversation.getId())
                        .userId(userId)
                        .role("user")
                        .content(request.getInitialMessage())
                        .messageType("text")
                        .timestamp(LocalDateTime.now())
                        .build();
                
                messageRepository.save(userMessage);
                conversationRepository.updateActivity(conversation.getId(), LocalDateTime.now());
            }
            
            // Publish event
            kafkaTemplate.send("conversation-events", "conversation.started",
                    Map.of("conversationId", conversation.getId(), "userId", userId, 
                           "characterId", request.getCharacterId()));
            
            log.info("Started conversation {} for user {} with character {}", 
                    conversation.getId(), userId, request.getCharacterId());
            
            return conversation;
        })
        .flatMap(conversation -> enhanceConversationResponse(conversation))
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<ConversationResponse> getConversation(UUID userId, UUID conversationId) {
        return Mono.fromCallable(() -> 
                conversationRepository.findByIdAndUserId(conversationId, userId)
                        .orElseThrow(() -> new ConversationNotFoundException("Conversation not found"))
        )
        .flatMap(this::enhanceConversationResponse)
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<MessageResponse> sendMessage(UUID userId, UUID conversationId, SendMessageRequest request) {
        return validateConversation(userId, conversationId)
                .flatMap(conversation -> {
                    // Save user message
                    Message userMessage = Message.builder()
                            .conversationId(conversationId)
                            .userId(userId)
                            .role("user")
                            .content(request.getContent())
                            .messageType(request.getMessageType() != null ? request.getMessageType() : "text")
                            .metadata(request.getMetadata())
                            .timestamp(LocalDateTime.now())
                            .build();
                    
                    return Mono.fromCallable(() -> messageRepository.save(userMessage))
                            .flatMap(savedMessage -> processAndGenerateResponse(conversation, savedMessage, request));
                })
                .doOnSuccess(response -> {
                    // Update conversation activity
                    conversationRepository.updateActivity(conversationId, LocalDateTime.now());
                    
                    // Track analytics
                    analyticsService.trackMessage(userId, conversationId, response);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Flux<StreamingMessageEvent> streamMessage(UUID userId, UUID conversationId, SendMessageRequest request) {
        return validateConversation(userId, conversationId)
                .flatMapMany(conversation -> {
                    // Create streaming session
                    String sessionId = UUID.randomUUID().toString();
                    StreamingSession session = StreamingSession.builder()
                            .sessionId(sessionId)
                            .userId(userId)
                            .conversationId(conversationId)
                            .connectionType("websocket")
                            .connectedAt(LocalDateTime.now())
                            .lastPingAt(LocalDateTime.now())
                            .isActive(true)
                            .build();
                    
                    sessionRepository.save(session);
                    
                    // Save user message
                    Message userMessage = Message.builder()
                            .conversationId(conversationId)
                            .userId(userId)
                            .role("user")
                            .content(request.getContent())
                            .messageType("text")
                            .metadata(request.getMetadata())
                            .timestamp(LocalDateTime.now())
                            .build();
                    
                    messageRepository.save(userMessage);
                    
                    // Stream AI response
                    return aiProcessor.streamResponse(conversation, userMessage, request.getOptions())
                            .map(chunk -> StreamingMessageEvent.builder()
                                    .eventType("message_chunk")
                                    .conversationId(conversationId.toString())
                                    .content(chunk.getContent())
                                    .chunkIndex(chunk.getIndex())
                                    .isComplete(chunk.isComplete())
                                    .timestamp(LocalDateTime.now())
                                    .build())
                            .doOnComplete(() -> {
                                sessionRepository.closeSession(sessionId, LocalDateTime.now());
                                conversationRepository.updateActivity(conversationId, LocalDateTime.now());
                            })
                            .doOnError(error -> {
                                log.error("Streaming error for conversation {}: {}", conversationId, error.getMessage());
                                sessionRepository.closeSession(sessionId, LocalDateTime.now());
                            });
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> endConversation(UUID userId, UUID conversationId) {
        return validateConversation(userId, conversationId)
                .flatMap(conversation -> Mono.fromCallable(() -> {
                    conversationRepository.endConversation(conversationId, "ended", LocalDateTime.now());
                    
                    // Deactivate context
                    contextRepository.findByConversationId(conversationId).ifPresent(context -> {
                        context.setActive(false);
                        contextRepository.save(context);
                    });
                    
                    // Save final analytics
                    analyticsService.finalizeConversationAnalytics(conversationId);
                    
                    // Update memory
                    memoryService.consolidateConversationMemory(userId, conversationId);
                    
                    // Publish event
                    kafkaTemplate.send("conversation-events", "conversation.ended",
                            Map.of("conversationId", conversationId, "userId", userId));
                    
                    return null;
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
    
    @Override
    public Mono<Page<ConversationResponse>> getUserConversations(UUID userId, Pageable pageable) {
        return Mono.fromCallable(() -> conversationRepository.findByUserIdOrderByLastActivityAtDesc(userId, pageable))
                .flatMap(page -> {
                    List<Mono<ConversationResponse>> enhancedConversations = page.getContent().stream()
                            .map(this::enhanceConversationResponse)
                            .collect(Collectors.toList());
                    
                    return Flux.merge(enhancedConversations)
                            .collectList()
                            .map(list -> new PageImpl<>(list, pageable, page.getTotalElements()));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Page<MessageResponse>> getConversationMessages(UUID userId, UUID conversationId, Pageable pageable) {
        return validateConversation(userId, conversationId)
                .flatMap(conversation -> Mono.fromCallable(() -> 
                        messageRepository.findByConversationIdOrderByTimestampDesc(conversationId, pageable)
                ))
                .map(page -> page.map(conversationMapper::toMessageResponse))
                .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<ConversationHistoryResponse> getConversationHistory(UUID userId, ConversationHistoryRequest request) {
        return Mono.fromCallable(() -> {
            List<Conversation> conversations;
            
            if (request.getCharacterId() != null) {
                conversations = conversationRepository.findByUserIdAndCharacterIdOrderByLastActivityAtDesc(
                        userId, request.getCharacterId(), Pageable.unpaged()).getContent();
            } else {
                conversations = conversationRepository.findByUserIdOrderByLastActivityAtDesc(
                        userId, Pageable.unpaged()).getContent();
            }
            
            // Filter by date range if provided
            if (request.getStartDate() != null || request.getEndDate() != null) {
                conversations = conversations.stream()
                        .filter(c -> {
                            boolean afterStart = request.getStartDate() == null || 
                                    !c.getStartedAt().isBefore(request.getStartDate());
                            boolean beforeEnd = request.getEndDate() == null || 
                                    !c.getStartedAt().isAfter(request.getEndDate());
                            return afterStart && beforeEnd;
                        })
                        .collect(Collectors.toList());
            }
            
            // Apply limit
            if (request.getLimit() != null && request.getLimit() > 0) {
                conversations = conversations.stream()
                        .limit(request.getLimit())
                        .collect(Collectors.toList());
            }
            
            // Calculate statistics
            ConversationStatistics statistics = analyticsService.calculateStatistics(userId, conversations);
            
            return ConversationHistoryResponse.builder()
                    .conversations(conversations.stream()
                            .map(conversationMapper::toResponse)
                            .collect(Collectors.toList()))
                    .totalConversations((long) conversations.size())
                    .statistics(statistics)
                    .build();
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<ConversationExportResponse> exportConversations(UUID userId, ConversationExportRequest request) {
        return Mono.fromCallable(() -> {
            // Validate user owns all requested conversations
            List<Conversation> conversations = conversationRepository.findAllById(request.getConversationIds());
            conversations.forEach(c -> {
                if (!c.getUserId().equals(userId)) {
                    throw new UnauthorizedException("Cannot export conversation: " + c.getId());
                }
            });
            
            // Export conversations
            return exportService.exportConversations(conversations, request);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Void> updateMemory(UUID userId, MemoryUpdateRequest request) {
        return Mono.fromCallable(() -> {
            memoryService.updateCharacterMemory(userId, request.getCharacterId(), request.getMemories());
            
            // Update character context
            characterContextRepository.findByUserIdAndCharacterId(userId, request.getCharacterId())
                    .ifPresent(context -> {
                        context.getSharedMemories().addAll(
                                request.getMemories().stream()
                                        .map(MemoryItem::getContent)
                                        .collect(Collectors.toList())
                        );
                        context.setLastUpdated(LocalDateTime.now());
                        characterContextRepository.save(context);
                    });
            
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Mono<ConversationStatistics> getConversationStatistics(UUID userId, UUID characterId) {
        return Mono.fromCallable(() -> {
            List<Conversation> conversations = conversationRepository
                    .findByUserIdAndCharacterIdOrderByLastActivityAtDesc(userId, characterId, Pageable.unpaged())
                    .getContent();
            
            return analyticsService.calculateStatistics(userId, conversations);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    private Mono<Conversation> validateConversation(UUID userId, UUID conversationId) {
        return Mono.fromCallable(() -> 
                conversationRepository.findByIdAndUserId(conversationId, userId)
                        .orElseThrow(() -> new ConversationNotFoundException("Conversation not found"))
        );
    }
    
    private Mono<MessageResponse> processAndGenerateResponse(Conversation conversation, 
                                                           Message userMessage, 
                                                           SendMessageRequest request) {
        long startTime = System.currentTimeMillis();
        
        return Mono.fromCallable(() -> {
            // Get conversation context
            ConversationContext context = contextRepository.findByConversationId(conversation.getId())
                    .orElseThrow(() -> new IllegalStateException("Conversation context not found"));
            
            // Get recent messages for context
            List<Message> recentMessages = messageRepository.findByConversationIdOrderByTimestampDesc(
                    conversation.getId(), Pageable.ofSize(10)).getContent();
            Collections.reverse(recentMessages);
            
            // Get character context
            CharacterContext characterContext = characterContextRepository
                    .findByUserIdAndCharacterId(conversation.getUserId(), conversation.getCharacterId())
                    .orElse(null);
            
            return aiProcessor.generateResponse(
                    conversation,
                    userMessage,
                    recentMessages,
                    context,
                    characterContext,
                    request.getOptions()
            );
        })
        .flatMap(aiResponse -> Mono.fromCallable(() -> {
            // Save AI response
            Message assistantMessage = Message.builder()
                    .conversationId(conversation.getId())
                    .userId(conversation.getUserId())
                    .role("assistant")
                    .content(aiResponse.getContent())
                    .messageType("text")
                    .metadata(new HashMap<>())
                    .timestamp(LocalDateTime.now())
                    .build();
            
            // Add emotion data if available
            if (aiResponse.getEmotion() != null) {
                assistantMessage.getMetadata().put("emotion", aiResponse.getEmotion());
            }
            
            // Add action data if available
            if (aiResponse.getAction() != null) {
                assistantMessage.getMetadata().put("action", aiResponse.getAction());
            }
            
            // Add processing time
            long processingTime = System.currentTimeMillis() - startTime;
            assistantMessage.getMetadata().put("processingTime", processingTime);
            
            Message savedMessage = messageRepository.save(assistantMessage);
            
            // Update memory if significant
            if (aiResponse.isSignificant()) {
                memoryService.extractAndStoreMemory(conversation, userMessage, savedMessage);
            }
            
            return conversationMapper.toMessageResponse(savedMessage);
        }))
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    private Mono<ConversationResponse> enhanceConversationResponse(Conversation conversation) {
        return Mono.fromCallable(() -> {
            ConversationResponse response = conversationMapper.toResponse(conversation);
            
            // Get character name
            characterClient.getCharacter(conversation.getCharacterId())
                    .ifPresent(character -> response.setCharacterName(character.getName()));
            
            // Get recent messages
            List<Message> recentMessages = messageRepository
                    .findByConversationIdOrderByTimestampDesc(conversation.getId(), Pageable.ofSize(5))
                    .getContent();
            
            response.setRecentMessages(recentMessages.stream()
                    .map(conversationMapper::toMessageResponse)
                    .collect(Collectors.toList()));
            
            // Get settings from context
            contextRepository.findByConversationId(conversation.getId())
                    .ifPresent(context -> response.setSettings(context.getSettings()));
            
            return response;
        });
    }
}
