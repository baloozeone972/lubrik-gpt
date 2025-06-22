package com.virtualcompanion.conversationservice.service;

public interface AIProcessorService {
    Mono<AIResponse> generateResponse(Conversation conversation,
                                      Message userMessage,
                                      List<Message> conversationHistory,
                                      ConversationContext conversationContext,
                                      CharacterContext characterContext,
                                      MessageOptions options);

    Flux<StreamChunk> streamResponse(Conversation conversation,
                                     Message userMessage,
                                     MessageOptions options);
}
