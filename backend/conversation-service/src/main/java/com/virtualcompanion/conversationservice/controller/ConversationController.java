package com.virtualcompanion.conversationservice.controller;

public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final AIProcessorService aiProcessor;
    private final MemoryService memoryService;
    private final StreamingService streamingService;
    
    // Gestion des flux SSE pour chaque utilisateur
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> userSinks = new ConcurrentHashMap<>();

    // ========== Conversation Management ==========

    @PostMapping
    @Operation(summary = "Créer une nouvelle conversation")
    public Mono<ConversationDto> createConversation(
            @Valid @RequestBody ConversationCreateDto createDto,
            @CurrentUser UserPrincipal currentUser) {
        
        log.info("Creating conversation for user: {} with character: {}", 
                currentUser.getId(), createDto.getCharacterId());
        
        return conversationService.createConversation(
                currentUser.getId(),
                createDto.getCharacterId(),
                createDto.getTitle()
        );
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Obtenir une conversation")
    public Mono<ConversationDetailDto> getConversation(
            @PathVariable String conversationId,
            @CurrentUser UserPrincipal currentUser) {
        
        return conversationService.getConversation(conversationId, currentUser.getId())
                .switchIfEmpty(Mono.error(new ConversationNotFoundException(conversationId)));
    }

    @GetMapping
    @Operation(summary = "Lister les conversations de l'utilisateur")
    public Mono<Page<ConversationSummaryDto>> getUserConversations(
            @RequestParam(required = false) String characterId,
            @RequestParam(required = false) Boolean active,
            Pageable pageable,
            @CurrentUser UserPrincipal currentUser) {
        
        return conversationService.getUserConversations(
                currentUser.getId(), 
                characterId, 
                active, 
                pageable
        );
    }

    @PutMapping("/{conversationId}")
    @Operation(summary = "Mettre à jour une conversation")
    public Mono<ConversationDto> updateConversation(
            @PathVariable String conversationId,
            @Valid @RequestBody ConversationUpdateDto updateDto,
            @CurrentUser UserPrincipal currentUser) {
        
        return conversationService.updateConversation(
                conversationId, 
                updateDto, 
                currentUser.getId()
        );
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "Supprimer une conversation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteConversation(
            @PathVariable String conversationId,
            @CurrentUser UserPrincipal currentUser) {
        
        return conversationService.deleteConversation(conversationId, currentUser.getId())
                .doOnSuccess(v -> log.info("Conversation deleted: {}", conversationId));
    }

    @PostMapping("/{conversationId}/archive")
    @Operation(summary = "Archiver une conversation")
    public Mono<Void> archiveConversation(
            @PathVariable String conversationId,
            @CurrentUser UserPrincipal currentUser) {
        
        return conversationService.archiveConversation(conversationId, currentUser.getId());
    }

    // ========== Message Management ==========

    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Envoyer un message")
    public Mono<MessageDto> sendMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody MessageCreateDto messageDto,
            @CurrentUser UserPrincipal currentUser) {
        
        return messageService.sendMessage(conversationId, messageDto, currentUser.getId())
                .flatMap(message -> {
                    // Déclencher la génération de réponse IA
                    return aiProcessor.generateResponse(conversationId, message)
                            .then(Mono.just(message));
                });
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Obtenir l'historique des messages")
    public Flux<MessageDto> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @CurrentUser UserPrincipal currentUser) {
        
        return messageService.getConversationMessages(
                conversationId, 
                currentUser.getId(), 
                page, 
                size
        );
    }

    @PutMapping("/{conversationId}/messages/{messageId}")
    @Operation(summary = "Modifier un message")
    public Mono<MessageDto> updateMessage(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @Valid @RequestBody MessageUpdateDto updateDto,
            @CurrentUser UserPrincipal currentUser) {
        
        return messageService.updateMessage(
                messageId, 
                updateDto, 
                currentUser.getId()
        );
    }

    @DeleteMapping("/{conversationId}/messages/{messageId}")
    @Operation(summary = "Supprimer un message")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMessage(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            @CurrentUser UserPrincipal currentUser) {
        
        return messageService.deleteMessage(messageId, currentUser.getId());
    }

    // ========== Streaming Endpoints ==========

    @GetMapping(value = "/{conversationId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream de messages en temps réel (SSE)")
    public Flux<ServerSentEvent<String>> streamConversation(
            @PathVariable String conversationId,
            @CurrentUser UserPrincipal currentUser) {
        
        String userId = currentUser.getId();
        
        // Créer ou récupérer le sink pour cet utilisateur
        Sinks.Many<ServerSentEvent<String>> sink = userSinks.computeIfAbsent(
                userId,
                k -> Sinks.many().multicast().onBackpressureBuffer()
        );
        
        // Envoyer un heartbeat toutes les 30 secondes
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(30))
                .map(i -> ServerSentEvent.<String>builder()
                        .event("heartbeat")
                        .data("ping")
                        .build());
        
        // Combiner le flux de messages avec le heartbeat
        return Flux.merge(sink.asFlux(), heartbeat)
                .doOnCancel(() -> {
                    log.info("SSE connection closed for user: {}", userId);
                    userSinks.remove(userId);
                });
    }

    @PostMapping("/{conversationId}/messages/stream")
    @Operation(summary = "Envoyer un message avec réponse streaming")
    public Flux<ServerSentEvent<StreamingMessageChunk>> sendMessageWithStreaming(
            @PathVariable String conversationId,
            @Valid @RequestBody MessageCreateDto messageDto,
            @CurrentUser UserPrincipal currentUser) {
        
        return messageService.sendMessage(conversationId, messageDto, currentUser.getId())
                .flatMapMany(userMessage -> {
                    // Stream la réponse IA token par token
                    return aiProcessor.streamResponse(conversationId, userMessage)
                            .map(chunk -> ServerSentEvent.<StreamingMessageChunk>builder()
                                    .event("message-chunk")
                                    .data(chunk)
                                    .build())
                            .concatWith(
                                    Flux.just(ServerSentEvent.<StreamingMessageChunk>builder()
                                            .event("message-complete")
                                            .data(StreamingMessageChunk.complete())
                                            .build())
                            );
                });
    }

    // ========== Memory & Context ==========

    @GetMapping("/{conversationId}/memory")
    @Operation(summary = "Obtenir la mémoire de conversation")
    public Mono<ConversationMemoryDto> getConversationMemory(
            @PathVariable String conversationId,
            @CurrentUser UserPrincipal currentUser) {
        
        return memoryService.getConversationMemory(conversationId, currentUser.getId());
    }

    @PostMapping("/{conversationId}/memory/add")
    @Operation(summary = "Ajouter un souvenir important")
    public Mono<Void> addMemory(
            @PathVariable String conversationId,
            @Valid @RequestBody MemoryEntryDto memoryEntry,
            @CurrentUser UserPrincipal currentUser) {
        
        return memoryService.addImportantMemory(
                conversationId, 
                memoryEntry, 
                currentUser.getId()
        );
    }

    @GetMapping("/{conversationId}/context")
    @Operation(summary = "Obtenir le contexte actuel")
    public Mono<ConversationContextDto> getConversationContext(
            @PathVariable String conversationId,
            @CurrentUser UserPrincipal currentUser) {
        
        return conversationService.getConversationContext(
                conversationId, 
                currentUser.getId()
        );
    }

    // ========== Voice & Video ==========

    @PostMapping("/{conversationId}/voice/start")
    @Operation(summary = "Démarrer une session vocale")
    public Mono<VoiceSessionDto> startVoiceSession(
            @PathVariable String conversationId,
            @CurrentUser UserPrincipal currentUser) {
        
        return streamingService.createVoiceSession(conversationId, currentUser.getId());
    }

    @PostMapping("/{conversationId}/video/start")
    @Operation(summary = "Démarrer une session vidéo")
    public Mono<VideoSessionDto> startVideoSession(
            @PathVariable String conversationId,
            @RequestBody VideoSessionRequestDto request,
            @CurrentUser UserPrincipal currentUser) {
        
        return streamingService.createVideoSession(
                conversationId, 
                currentUser.getId(), 
                request
        );
    }

    // ========== Analytics & Export ==========

    @GetMapping("/{conversationId}/stats")
    @Operation(summary = "Obtenir les statistiques de conversation")
    public Mono<ConversationStatsDto> getConversationStats(
            @PathVariable String conversationId,
            @CurrentUser UserPrincipal currentUser) {
        
        return conversationService.getConversationStats(
                conversationId, 
                currentUser.getId()
        );
    }

    @GetMapping("/{conversationId}/export")
    @Operation(summary = "Exporter une conversation")
    public Mono<ResponseEntity<byte[]>> exportConversation(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "json") String format,
            @CurrentUser UserPrincipal currentUser) {
        
        return conversationService.exportConversation(
                conversationId, 
                currentUser.getId(), 
                format
        ).map(data -> {
            MediaType contentType = format.equals("pdf") 
                    ? MediaType.APPLICATION_PDF 
                    : MediaType.APPLICATION_JSON;
            
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header("Content-Disposition", 
                            "attachment; filename=conversation-" + conversationId + "." + format)
                    .body(data);
        });
    }

    // ========== Utility Methods ==========

    @PostMapping("/{conversationId}/regenerate-last")
    @Operation(summary = "Régénérer la dernière réponse IA")
    public Flux<ServerSentEvent<StreamingMessageChunk>> regenerateLastResponse(
            @PathVariable String conversationId,
            @CurrentUser UserPrincipal currentUser) {
        
        return conversationService.regenerateLastAIResponse(
                conversationId, 
                currentUser.getId()
        ).flatMapMany(message -> 
                aiProcessor.streamResponse(conversationId, message)
                        .map(chunk -> ServerSentEvent.<StreamingMessageChunk>builder()
                                .event("message-chunk")
                                .data(chunk)
                                .build())
        );
    }

    @PostMapping("/{conversationId}/clear")
    @Operation(summary = "Effacer l'historique de conversation")
    public Mono<Void> clearConversation(
            @PathVariable String conversationId,
            @CurrentUser UserPrincipal currentUser) {
        
        return conversationService.clearConversationHistory(
                conversationId, 
                currentUser.getId()
        );
    }
}
