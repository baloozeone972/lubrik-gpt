package com.virtualcompanion.conversationservice.service;

public interface ConversationService {

    Mono<ConversationResponse> startConversation(UUID userId, StartConversationRequest request);

    Mono<ConversationResponse> getConversation(UUID userId, UUID conversationId);

    Mono<MessageResponse> sendMessage(UUID userId, UUID conversationId, SendMessageRequest request);

    Flux<StreamingMessageEvent> streamMessage(UUID userId, UUID conversationId, SendMessageRequest request);

    Mono<Void> endConversation(UUID userId, UUID conversationId);

    Mono<Page<ConversationResponse>> getUserConversations(UUID userId, Pageable pageable);

    Mono<Page<MessageResponse>> getConversationMessages(UUID userId, UUID conversationId, Pageable pageable);

    Mono<ConversationHistoryResponse> getConversationHistory(UUID userId, ConversationHistoryRequest request);

    Mono<ConversationExportResponse> exportConversations(UUID userId, ConversationExportRequest request);

    Mono<Void> updateMemory(UUID userId, MemoryUpdateRequest request);

    Mono<ConversationStatistics> getConversationStatistics(UUID userId, UUID characterId);
}
