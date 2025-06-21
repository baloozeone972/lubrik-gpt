package com.virtualcompanion.conversationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualcompanion.characterservice.client.CharacterServiceClient;
import com.virtualcompanion.common.dto.CharacterDto;
import com.virtualcompanion.conversationservice.config.AIConfiguration;
import com.virtualcompanion.conversationservice.entity.Message;
import com.virtualcompanion.conversationservice.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIProcessorService {

    private final WebClient ollamaClient;
    private final AIConfiguration aiConfig;
    private final MessageRepository messageRepository;
    private final ConversationContextService contextService;
    private final MemoryService memoryService;
    private final CharacterServiceClient characterClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // ========== Response Generation ==========

    public Mono<MessageDto> generateResponse(String conversationId, MessageDto userMessage) {
        return contextService.buildConversationContext(conversationId)
                .flatMap(context -> {
                    // Vérifier le cache
                    String cacheKey = generateCacheKey(context, userMessage.getContent());
                    
                    return checkCache(cacheKey)
                            .switchIfEmpty(
                                    generateAIResponse(context, userMessage)
                                            .flatMap(response -> cacheResponse(cacheKey, response))
                            )
                            .flatMap(response -> saveAIMessage(conversationId, response, userMessage.getId()));
                })
                .doOnError(error -> log.error("Error generating AI response: ", error))
                .onErrorResume(error -> generateFallbackResponse(conversationId, userMessage));
    }

    public Flux<StreamingMessageChunk> streamResponse(String conversationId, MessageDto userMessage) {
        return contextService.buildConversationContext(conversationId)
                .flatMapMany(context -> streamFromOllama(context, userMessage.getContent()))
                .map(chunk -> StreamingMessageChunk.builder()
                        .content(chunk)
                        .timestamp(LocalDateTime.now())
                        .isComplete(false)
                        .build())
                .concatWith(Mono.just(StreamingMessageChunk.complete()))
                .doOnNext(chunk -> {
                    if (!chunk.isComplete()) {
                        publishToSSE(conversationId, chunk);
                    }
                })
                .onErrorResume(error -> {
                    log.error("Streaming error: ", error);
                    return Flux.just(StreamingMessageChunk.error(error.getMessage()));
                });
    }

    // ========== Core AI Methods ==========

    private Mono<String> generateAIResponse(ConversationContext context, MessageDto userMessage) {
        OllamaRequest request = buildOllamaRequest(context, userMessage.getContent());
        
        return ollamaClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), OllamaRequest.class)
                .retrieve()
                .bodyToMono(OllamaResponse.class)
                .timeout(Duration.ofSeconds(aiConfig.getResponseTimeout()))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .map(OllamaResponse::getResponse)
                .flatMap(response -> postProcessResponse(response, context));
    }

    private Flux<String> streamFromOllama(ConversationContext context, String userMessage) {
        OllamaRequest request = buildOllamaRequest(context, userMessage);
        request.setStream(true);
        
        AtomicInteger tokenCount = new AtomicInteger(0);
        StringBuilder fullResponse = new StringBuilder();
        
        return ollamaClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), OllamaRequest.class)
                .retrieve()
                .bodyToFlux(String.class)
                .map(chunk -> {
                    try {
                        JsonNode node = objectMapper.readTree(chunk);
                        String token = node.get("response").asText();
                        fullResponse.append(token);
                        tokenCount.incrementAndGet();
                        return token;
                    } catch (Exception e) {
                        log.error("Error parsing stream chunk: ", e);
                        return "";
                    }
                })
                .filter(token -> !token.isEmpty())
                .doOnComplete(() -> {
                    // Sauvegarder la réponse complète
                    saveStreamedResponse(context.getConversationId(), fullResponse.toString());
                    // Mettre à jour les métriques
                    updateTokenMetrics(context.getConversationId(), tokenCount.get());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    // ========== Request Building ==========

    private OllamaRequest buildOllamaRequest(ConversationContext context, String userMessage) {
        CharacterDto character = context.getCharacter();
        
        // Construire le prompt système basé sur la personnalité
        String systemPrompt = buildSystemPrompt(character, context);
        
        // Construire l'historique de conversation
        String conversationHistory = buildConversationHistory(context);
        
        // Ajouter les souvenirs pertinents
        String relevantMemories = context.getRelevantMemories().isEmpty() ? "" 
                : "\n\nSouvenirs importants:\n" + formatMemories(context.getRelevantMemories());
        
        String fullPrompt = conversationHistory + relevantMemories + 
                "\nUtilisateur: " + userMessage + "\nAssistant:";
        
        return OllamaRequest.builder()
                .model(aiConfig.getModel())
                .prompt(fullPrompt)
                .system(systemPrompt)
                .temperature(calculateTemperature(character))
                .topP(aiConfig.getTopP())
                .topK(aiConfig.getTopK())
                .numPredict(aiConfig.getMaxTokens())
                .repeatPenalty(1.1f)
                .stream(false)
                .options(buildModelOptions(character))
                .build();
    }

    private String buildSystemPrompt(CharacterDto character, ConversationContext context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Tu es ").append(character.getName()).append(". ");
        prompt.append(character.getDescription()).append("\n\n");
        
        // Personnalité
        if (character.getPersonality() != null) {
            prompt.append("Personnalité:\n");
            prompt.append("- Extraversion: ").append(character.getPersonality().getExtraversion()).append("/100\n");
            prompt.append("- Agréabilité: ").append(character.getPersonality().getAgreeableness()).append("/100\n");
            prompt.append("- Conscience: ").append(character.getPersonality().getConscientiousness()).append("/100\n");
            prompt.append("- Stabilité émotionnelle: ").append(character.getPersonality().getEmotionalStability()).append("/100\n");
            prompt.append("- Ouverture: ").append(character.getPersonality().getOpenness()).append("/100\n\n");
        }
        
        // Instructions comportementales
        prompt.append("Instructions:\n");
        prompt.append("- Reste toujours dans le personnage\n");
        prompt.append("- Utilise le style de communication approprié\n");
        prompt.append("- Tiens compte de l'historique de conversation\n");
        prompt.append("- Sois cohérent avec les interactions précédentes\n");
        
        // Contexte spécifique
        if (context.getCurrentMood() != null) {
            prompt.append("\nHumeur actuelle: ").append(context.getCurrentMood()).append("\n");
        }
        
        if (context.getRelationshipLevel() > 0) {
            prompt.append("Niveau de relation: ").append(context.getRelationshipLevel()).append("/100\n");
        }
        
        return prompt.toString();
    }

    private String buildConversationHistory(ConversationContext context) {
        StringBuilder history = new StringBuilder();
        
        // Limiter l'historique aux N derniers messages
        int maxMessages = aiConfig.getContextWindowMessages();
        List<MessageDto> recentMessages = context.getRecentMessages();
        
        if (recentMessages.size() > maxMessages) {
            recentMessages = recentMessages.subList(
                    recentMessages.size() - maxMessages, 
                    recentMessages.size()
            );
        }
        
        for (MessageDto msg : recentMessages) {
            String role = msg.getRole() == MessageRole.USER ? "Utilisateur" : context.getCharacter().getName();
            history.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        
        return history.toString();
    }

    // ========== Post-Processing ==========

    private Mono<String> postProcessResponse(String response, ConversationContext context) {
        return Mono.fromCallable(() -> {
            // Nettoyer la réponse
            String cleaned = cleanResponse(response);
            
            // Vérifier la cohérence avec le personnage
            if (!isResponseCoherent(cleaned, context)) {
                log.warn("Response not coherent with character, regenerating...");
                return regenerateWithConstraints(context, cleaned);
            }
            
            // Appliquer des filtres de sécurité
            cleaned = applySecurityFilters(cleaned, context);
            
            // Ajouter des éléments de personnalité
            cleaned = enhanceWithPersonality(cleaned, context.getCharacter());
            
            return cleaned;
        })
        .flatMap(result -> result instanceof Mono ? (Mono<String>) result : Mono.just((String) result))
        .subscribeOn(Schedulers.boundedElastic());
    }

    private String cleanResponse(String response) {
        // Supprimer les répétitions
        response = removeRepetitions(response);
        
        // Corriger la ponctuation
        response = fixPunctuation(response);
        
        // Limiter la longueur si nécessaire
        if (response.length() > aiConfig.getMaxResponseLength()) {
            response = truncateNaturally(response, aiConfig.getMaxResponseLength());
        }
        
        return response.trim();
    }

    // ========== Caching ==========

    private String generateCacheKey(ConversationContext context, String message) {
        String characterId = context.getCharacter().getId();
        String messageHash = Integer.toHexString(message.hashCode());
        String contextHash = Integer.toHexString(context.getRecentMessages().hashCode());
        
        return String.format("ai:response:%s:%s:%s", characterId, messageHash, contextHash);
    }

    private Mono<String> checkCache(String cacheKey) {
        return redisTemplate.opsForValue().get(cacheKey)
                .doOnNext(cached -> log.debug("Cache hit for key: {}", cacheKey));
    }

    private Mono<String> cacheResponse(String cacheKey, String response) {
        return redisTemplate.opsForValue()
                .set(cacheKey, response, Duration.ofHours(aiConfig.getCacheTtlHours()))
                .thenReturn(response);
    }

    // ========== Message Saving ==========

    private Mono<MessageDto> saveAIMessage(String conversationId, String content, String inReplyTo) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .content(content)
                .role(MessageRole.ASSISTANT)
                .inReplyTo(inReplyTo)
                .metadata(MessageMetadata.builder()
                        .model(aiConfig.getModel())
                        .tokenCount(countTokens(content))
                        .generationTime(System.currentTimeMillis())
                        .build())
                .build();
        
        return messageRepository.save(message)
                .map(this::toDto)
                .doOnSuccess(msg -> updateConversationActivity(conversationId));
    }

    // ========== Fallback Handling ==========

    private Mono<MessageDto> generateFallbackResponse(String conversationId, MessageDto userMessage) {
        String fallbackContent = selectFallbackResponse(userMessage.getContent());
        
        return saveAIMessage(conversationId, fallbackContent, userMessage.getId())
                .doOnSuccess(msg -> log.info("Used fallback response for conversation: {}", conversationId));
    }

    private String selectFallbackResponse(String userMessage) {
        List<String> fallbacks = Arrays.asList(
                "Je suis désolé, je n'ai pas bien compris. Pouvez-vous reformuler ?",
                "Hmm, laissez-moi réfléchir... Pourriez-vous m'en dire plus ?",
                "C'est intéressant ! Qu'est-ce qui vous fait penser à ça ?",
                "Je suis là pour vous écouter. Continuez, je vous prie.",
                "Voilà une question fascinante. Explorons cela ensemble."
        );
        
        // Sélectionner une réponse basée sur le hash du message
        int index = Math.abs(userMessage.hashCode()) % fallbacks.size();
        return fallbacks.get(index);
    }

    // ========== Utility Methods ==========

    private double calculateTemperature(CharacterDto character) {
        if (character.getPersonality() == null) {
            return aiConfig.getDefaultTemperature();
        }
        
        // Ajuster la température selon la personnalité
        double baseTemp = aiConfig.getDefaultTemperature();
        double creativity = character.getPersonality().getOpenness() / 100.0;
        double stability = character.getPersonality().getEmotionalStability() / 100.0;
        
        // Plus créatif = température plus élevée
        // Plus stable = température plus basse
        return baseTemp + (creativity * 0.3) - (stability * 0.2);
    }

    private Map<String, Object> buildModelOptions(CharacterDto character) {
        Map<String, Object> options = new HashMap<>();
        
        // Options spécifiques au modèle
        options.put("mirostat", 2);
        options.put("mirostat_eta", 0.1);
        options.put("mirostat_tau", 5.0);
        
        // Ajuster selon le type de personnage
        if (character.getCategory() == CharacterCategory.ROMANTIC) {
            options.put("temperature", 0.8);
            options.put("top_p", 0.95);
        } else if (character.getCategory() == CharacterCategory.MENTOR) {
            options.put("temperature", 0.3);
            options.put("top_p", 0.9);
        }
        
        return options;
    }

    private int countTokens(String text) {
        // Estimation simple : ~4 caractères par token
        return text.length() / 4;
    }

    private void publishToSSE(String conversationId, StreamingMessageChunk chunk) {
        // Publier vers le service de streaming pour SSE
        streamingService.publishChunk(conversationId, chunk);
    }

    private void updateConversationActivity(String conversationId) {
        conversationService.updateLastActivity(conversationId).subscribe();
    }

    private MessageDto toDto(Message message) {
        return MessageDto.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .content(message.getContent())
                .role(message.getRole())
                .timestamp(message.getCreatedAt())
                .metadata(message.getMetadata())
                .build();
    }
}