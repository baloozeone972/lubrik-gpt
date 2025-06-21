// CharacterSearchService.java
package com.virtualcompanion.characterservice.service;

import com.virtualcompanion.characterservice.dto.CharacterSearchRequest;
import com.virtualcompanion.characterservice.dto.CharacterSearchResponse;
import com.virtualcompanion.characterservice.entity.Character;

import java.util.UUID;

public interface CharacterSearchService {
    void indexCharacter(Character character);
    void updateCharacter(Character character);
    void deleteCharacter(UUID characterId);
    CharacterSearchResponse searchCharacters(CharacterSearchRequest request);
}

// CharacterSearchServiceImpl.java
package com.virtualcompanion.characterservice.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import com.virtualcompanion.characterservice.dto.CharacterResponse;
import com.virtualcompanion.characterservice.dto.CharacterSearchRequest;
import com.virtualcompanion.characterservice.dto.CharacterSearchResponse;
import com.virtualcompanion.characterservice.entity.Character;
import com.virtualcompanion.characterservice.mapper.CharacterMapper;
import com.virtualcompanion.characterservice.service.CharacterSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterSearchServiceImpl implements CharacterSearchService {
    
    private final ElasticsearchClient esClient;
    private final CharacterMapper characterMapper;
    
    @Value("${character.search.index-name}")
    private String indexName;
    
    @Override
    public void indexCharacter(Character character) {
        try {
            Map<String, Object> document = buildCharacterDocument(character);
            
            IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(character.getId().toString())
                    .document(document)
            );
            
            IndexResponse response = esClient.index(request);
            log.info("Character indexed: {} with result: {}", character.getId(), response.result());
            
        } catch (IOException e) {
            log.error("Failed to index character: {}", character.getId(), e);
        }
    }
    
    @Override
    public void updateCharacter(Character character) {
        try {
            Map<String, Object> document = buildCharacterDocument(character);
            
            UpdateRequest<Map<String, Object>, Map<String, Object>> request = UpdateRequest.of(u -> u
                    .index(indexName)
                    .id(character.getId().toString())
                    .doc(document)
                    .docAsUpsert(true)
            );
            
            UpdateResponse<Map<String, Object>> response = esClient.update(request, Map.class);
            log.info("Character updated in index: {} with result: {}", character.getId(), response.result());
            
        } catch (IOException e) {
            log.error("Failed to update character in index: {}", character.getId(), e);
        }
    }
    
    @Override
    public void deleteCharacter(UUID characterId) {
        try {
            DeleteRequest request = DeleteRequest.of(d -> d
                    .index(indexName)
                    .id(characterId.toString())
            );
            
            DeleteResponse response = esClient.delete(request);
            log.info("Character deleted from index: {} with result: {}", characterId, response.result());
            
        } catch (IOException e) {
            log.error("Failed to delete character from index: {}", characterId, e);
        }
    }
    
    @Override
    public CharacterSearchResponse searchCharacters(CharacterSearchRequest request) {
        try {
            // Build query
            Query query = buildSearchQuery(request);
            
            // Build aggregations
            Map<String, Aggregation> aggregations = buildAggregations();
            
            // Execute search
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .query(query)
                    .aggregations(aggregations)
                    .from(request.getPage() * request.getSize())
                    .size(request.getSize())
                    .sort(buildSortOptions(request))
            );
            
            SearchResponse<Map> response = esClient.search(searchRequest, Map.class);
            
            // Process results
            List<CharacterResponse> characters = response.hits().hits().stream()
                    .map(this::mapHitToCharacter)
                    .collect(Collectors.toList());
            
            // Process facets
            Map<String, Map<String, Long>> facets = processFacets(response.aggregations());
            
            return CharacterSearchResponse.builder()
                    .characters(characters)
                    .totalElements(response.hits().total().value())
                    .totalPages((int) Math.ceil((double) response.hits().total().value() / request.getSize()))
                    .currentPage(request.getPage())
                    .pageSize(request.getSize())
                    .facets(facets)
                    .build();
            
        } catch (IOException e) {
            log.error("Search failed", e);
            return CharacterSearchResponse.builder()
                    .characters(new ArrayList<>())
                    .totalElements(0L)
                    .totalPages(0)
                    .currentPage(0)
                    .pageSize(request.getSize())
                    .facets(new HashMap<>())
                    .build();
        }
    }
    
    private Map<String, Object> buildCharacterDocument(Character character) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", character.getId().toString());
        doc.put("name", character.getName());
        doc.put("description", character.getDescription());
        doc.put("category", character.getCategory());
        doc.put("gender", character.getGender().toString());
        doc.put("age", character.getAge());
        doc.put("backstory", character.getBackstory());
        doc.put("tags", character.getTags().stream().map(t -> t.getName()).collect(Collectors.toList()));
        doc.put("isPublic", character.getIsPublic());
        doc.put("isNsfw", character.getIsNsfw());
        doc.put("creatorId", character.getCreatorId().toString());
        doc.put("averageRating", character.getAverageRating());
        doc.put("totalRatings", character.getTotalRatings());
        doc.put("popularityScore", character.getPopularityScore());
        doc.put("createdAt", character.getCreatedAt().toString());
        doc.put("updatedAt", character.getUpdatedAt().toString());
        
        return doc;
    }
    
    private Query buildSearchQuery(CharacterSearchRequest request) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        
        // Must queries
        List<Query> mustQueries = new ArrayList<>();
        mustQueries.add(TermQuery.of(t -> t.field("isPublic").value(true))._toQuery());
        
        // Text search
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            mustQueries.add(MultiMatchQuery.of(m -> m
                    .query(request.getQuery())
                    .fields("name^3", "description^2", "backstory", "tags")
                    .type(TextQueryType.BestFields)
            )._toQuery());
        }
        
        // Filters
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            mustQueries.add(TermsQuery.of(t -> t
                    .field("category")
                    .terms(TermsQueryField.of(tf -> tf.value(request.getCategories().stream()
                            .map(c -> FieldValue.of(c))
                            .collect(Collectors.toList()))))
            )._toQuery());
        }
        
        if (request.getGender() != null) {
            mustQueries.add(TermQuery.of(t -> t.field("gender").value(request.getGender()))._toQuery());
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            mustQueries.add(TermsQuery.of(t -> t
                    .field("tags")
                    .terms(TermsQueryField.of(tf -> tf.value(request.getTags().stream()
                            .map(tag -> FieldValue.of(tag))
                            .collect(Collectors.toList()))))
            )._toQuery());
        }
        
        // Range filters
        if (request.getMinAge() != null || request.getMaxAge() != null) {
            RangeQuery.Builder rangeQuery = new RangeQuery.Builder().field("age");
            if (request.getMinAge() != null) {
                rangeQuery.gte(JsonData.of(request.getMinAge()));
            }
            if (request.getMaxAge() != null) {
                rangeQuery.lte(JsonData.of(request.getMaxAge()));
            }
            mustQueries.add(rangeQuery.build()._toQuery());
        }
        
        if (request.getMinRating() != null) {
            mustQueries.add(RangeQuery.of(r -> r
                    .field("averageRating")
                    .gte(JsonData.of(request.getMinRating()))
            )._toQuery());
        }
        
        if (request.getIsNsfw() != null) {
            mustQueries.add(TermQuery.of(t -> t.field("isNsfw").value(request.getIsNsfw()))._toQuery());
        }
        
        boolQuery.must(mustQueries);
        
        return boolQuery.build()._toQuery();
    }
    
    private Map<String, Aggregation> buildAggregations() {
        Map<String, Aggregation> aggs = new HashMap<>();
        
        aggs.put("categories", Aggregation.of(a -> a
                .terms(t -> t.field("category").size(20))
        ));
        
        aggs.put("tags", Aggregation.of(a -> a
                .terms(t -> t.field("tags").size(50))
        ));
        
        aggs.put("gender", Aggregation.of(a -> a
                .terms(t -> t.field("gender").size(10))
        ));
        
        aggs.put("age_ranges", Aggregation.of(a -> a
                .range(r -> r
                        .field("age")
                        .ranges(
                                RangeAggregationRange.of(ar -> ar.key("18-25").from("18").to("25")),
                                RangeAggregationRange.of(ar -> ar.key("26-35").from("26").to("35")),
                                RangeAggregationRange.of(ar -> ar.key("36-45").from("36").to("45")),
                                RangeAggregationRange.of(ar -> ar.key("46+").from("46"))
                        )
                )
        ));
        
        return aggs;
    }
    
    private List<SortOptions> buildSortOptions(CharacterSearchRequest request) {
        List<SortOptions> sortOptions = new ArrayList<>();
        
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "popularityScore";
        SortOrder sortOrder = "ASC".equalsIgnoreCase(request.getSortDirection()) ? 
                SortOrder.Asc : SortOrder.Desc;
        
        sortOptions.add(SortOptions.of(s -> s
                .field(f -> f.field(sortBy).order(sortOrder))
        ));
        
        return sortOptions;
    }
    
    private CharacterResponse mapHitToCharacter(Hit<Map> hit) {
        Map<String, Object> source = hit.source();
        
        return CharacterResponse.builder()
                .id(UUID.fromString((String) source.get("id")))
                .name((String) source.get("name"))
                .description((String) source.get("description"))
                .category((String) source.get("category"))
                .gender((String) source.get("gender"))
                .age((Integer) source.get("age"))
                .backstory((String) source.get("backstory"))
                .tags((List<String>) source.get("tags"))
                .averageRating((Double) source.get("averageRating"))
                .totalRatings(((Number) source.get("totalRatings")).longValue())
                .build();
    }
    
    private Map<String, Map<String, Long>> processFacets(Map<String, Aggregate> aggregations) {
        Map<String, Map<String, Long>> facets = new HashMap<>();
        
        // Process each aggregation type
        aggregations.forEach((name, aggregate) -> {
            Map<String, Long> buckets = new HashMap<>();
            
            if (aggregate.isSterms()) {
                aggregate.sterms().buckets().array().forEach(bucket -> {
                    buckets.put(bucket.key().stringValue(), bucket.docCount());
                });
            } else if (aggregate.isRange()) {
                aggregate.range().buckets().array().forEach(bucket -> {
                    buckets.put(bucket.key(), bucket.docCount());
                });
            }
            
            facets.put(name, buckets);
        });
        
        return facets;
    }
}

// ImageStorageService.java
package com.virtualcompanion.characterservice.service;

import com.virtualcompanion.characterservice.entity.CharacterImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ImageStorageService {
    CharacterImage uploadImage(UUID characterId, MultipartFile file);
    void deleteImage(CharacterImage image);
    String generateThumbnail(String originalUrl);
}

// ImageStorageServiceImpl.java
package com.virtualcompanion.characterservice.service.impl;

import com.virtualcompanion.characterservice.entity.CharacterImage;
import com.virtualcompanion.characterservice.exception.InvalidFileException;
import com.virtualcompanion.characterservice.service.ImageStorageService;
import io.minio.*;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageServiceImpl implements ImageStorageService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket.characters}")
    private String bucketName;
    
    @Value("${minio.endpoint}")
    private String minioEndpoint;
    
    @Value("${character.defaults.max-image-size}")
    private long maxImageSize;
    
    @Value("${character.defaults.allowed-image-types}")
    private List<String> allowedImageTypes;
    
    @Override
    public CharacterImage uploadImage(UUID characterId, MultipartFile file) {
        // Validate file
        validateFile(file);
        
        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String filename = characterId + "/" + UUID.randomUUID() + extension;
            
            // Upload original image
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            
            String imageUrl = minioEndpoint + "/" + bucketName + "/" + filename;
            
            // Generate and upload thumbnail
            String thumbnailFilename = characterId + "/thumb_" + UUID.randomUUID() + extension;
            byte[] thumbnailData = generateThumbnailBytes(file);
            
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(thumbnailFilename)
                            .stream(new ByteArrayInputStream(thumbnailData), thumbnailData.length, -1)
                            .contentType(file.getContentType())
                            .build()
            );
            
            String thumbnailUrl = minioEndpoint + "/" + bucketName + "/" + thumbnailFilename;
            
            // Create image entity
            return CharacterImage.builder()
                    .characterId(characterId)
                    .imageUrl(imageUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .fileName(originalFilename)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .imageType("avatar")
                    .isPrimary(false)
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to upload image for character: {}", characterId, e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }
    
    @Override
    public void deleteImage(CharacterImage image) {
        try {
            // Extract object name from URL
            String objectName = extractObjectName(image.getImageUrl());
            String thumbnailObjectName = extractObjectName(image.getThumbnailUrl());
            
            // Delete original image
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            
            // Delete thumbnail
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(thumbnailObjectName)
                            .build()
            );
            
            log.info("Deleted image and thumbnail for character: {}", image.getCharacterId());
            
        } catch (Exception e) {
            log.error("Failed to delete image: {}", image.getId(), e);
            throw new RuntimeException("Failed to delete image", e);
        }
    }
    
    @Override
    public String generateThumbnail(String originalUrl) {
        // This would be used for generating thumbnails for existing images
        // Implementation would download, resize, and re-upload
        return originalUrl; // Simplified for now
    }
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }
        
        if (file.getSize() > maxImageSize) {
            throw new InvalidFileException("File size exceeds maximum allowed size of " + maxImageSize + " bytes");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !allowedImageTypes.contains(contentType)) {
            throw new InvalidFileException("File type not allowed. Allowed types: " + allowedImageTypes);
        }
    }
    
    private byte[] generateThumbnailBytes(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(originalImage)
                .size(200, 200)
                .keepAspectRatio(true)
                .outputFormat("jpg")
                .outputQuality(0.8)
                .toOutputStream(outputStream);
        
        return outputStream.toByteArray();
    }
    
    private String extractObjectName(String url) {
        // Extract object name from MinIO URL
        String prefix = minioEndpoint + "/" + bucketName + "/";
        return url.substring(prefix.length());
    }
}

// PersonalityEngineService.java
package com.virtualcompanion.characterservice.service;

import com.virtualcompanion.characterservice.dto.GenerateCharacterRequest;
import com.virtualcompanion.characterservice.entity.Character;
import com.virtualcompanion.characterservice.entity.CharacterPersonality;
import com.virtualcompanion.characterservice.entity.UserCharacter;

import java.util.List;
import java.util.UUID;

public interface PersonalityEngineService {
    Character generateCharacter(GenerateCharacterRequest request);
    CharacterPersonality generatePersonality(GenerateCharacterRequest request);
    List<Character> getRecommendations(UUID userId, List<UserCharacter> userHistory, int limit);
    double calculatePopularityScore(long conversations, Double rating, Long ratingCount, long uniqueUsers);
}

// PersonalityEngineServiceImpl.java
package com.virtualcompanion.characterservice.service.impl;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.generate.TextGenerator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.virtualcompanion.characterservice.dto.GenerateCharacterRequest;
import com.virtualcompanion.characterservice.entity.Character;
import com.virtualcompanion.characterservice.entity.CharacterPersonality;
import com.virtualcompanion.characterservice.entity.UserCharacter;
import com.virtualcompanion.characterservice.repository.CharacterRepository;
import com.virtualcompanion.characterservice.service.PersonalityEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalityEngineServiceImpl implements PersonalityEngineService {
    
    private final CharacterRepository characterRepository;
    
    @Override
    public Character generateCharacter(GenerateCharacterRequest request) {
        log.info("Generating character from prompt: {}", request.getPrompt());
        
        // This is a simplified implementation
        // In production, this would use a more sophisticated AI model
        
        Character character = Character.builder()
                .name(generateName(request.getPrompt()))
                .description(generateDescription(request.getPrompt()))
                .category(request.getCategory() != null ? request.getCategory() : "CUSTOM")
                .gender(parseGender(request.getGender()))
                .age(generateAge(request.getPrompt()))
                .backstory(generateBackstory(request.getPrompt()))
                .isPublic(false)
                .isNsfw(false)
                .build();
        
        return character;
    }
    
    @Override
    public CharacterPersonality generatePersonality(GenerateCharacterRequest request) {
        // Generate personality traits based on the prompt
        // This would use ML models in production
        
        Random random = new Random();
        
        CharacterPersonality personality = CharacterPersonality.builder()
                .openness(request.getPreferredTraits() != null && request.getPreferredTraits().getOpenness() != null ?
                        request.getPreferredTraits().getOpenness() : 0.5 + (random.nextDouble() - 0.5) * 0.4)
                .conscientiousness(request.getPreferredTraits() != null && request.getPreferredTraits().getConscientiousness() != null ?
                        request.getPreferredTraits().getConscientiousness() : 0.5 + (random.nextDouble() - 0.5) * 0.4)
                .extraversion(request.getPreferredTraits() != null && request.getPreferredTraits().getExtraversion() != null ?
                        request.getPreferredTraits().getExtraversion() : 0.5 + (random.nextDouble() - 0.5) * 0.4)
                .agreeableness(request.getPreferredTraits() != null && request.getPreferredTraits().getAgreeableness() != null ?
                        request.getPreferredTraits().getAgreeableness() : 0.5 + (random.nextDouble() - 0.5) * 0.4)
                .neuroticism(request.getPreferredTraits() != null && request.getPreferredTraits().getNeuroticism() != null ?
                        request.getPreferredTraits().getNeuroticism() : 0.5 + (random.nextDouble() - 0.5) * 0.4)
                .dominantTrait(determineDominantTrait(personality))
                .behaviorNotes(generateBehaviorNotes(request.getPrompt()))
                .build();
        
        return personality;
    }
    
    @Override
    public List<Character> getRecommendations(UUID userId, List<UserCharacter> userHistory, int limit) {
        // Analyze user's interaction history to find patterns
        Set<String> preferredCategories = new HashSet<>();
        Map<String, Integer> tagFrequency = new HashMap<>();
        
        for (UserCharacter uc : userHistory) {
            characterRepository.findById(uc.getCharacterId()).ifPresent(character -> {
                preferredCategories.add(character.getCategory());
                character.getTags().forEach(tag -> 
                    tagFrequency.merge(tag.getName(), 1, Integer::sum)
                );
            });
        }
        
        // Find similar characters
        List<Character> recommendations = characterRepository.findAll().stream()
                .filter(c -> c.getIsActive() && c.getIsPublic())
                .filter(c -> !userHistory.stream().anyMatch(uc -> uc.getCharacterId().equals(c.getId())))
                .sorted((c1, c2) -> {
                    int score1 = calculateRecommendationScore(c1, preferredCategories, tagFrequency);
                    int score2 = calculateRecommendationScore(c2, preferredCategories, tagFrequency);
                    return Integer.compare(score2, score1);
                })
                .limit(limit)
                .collect(Collectors.toList());
        
        return recommendations;
    }
    
    @Override
    public double calculatePopularityScore(long conversations, Double rating, Long ratingCount, long uniqueUsers) {
        // Weighted formula for popularity
        double conversationScore = Math.log1p(conversations) * 0.3;
        double ratingScore = (rating != null ? rating : 0) * 0.4;
        double engagementScore = Math.log1p(uniqueUsers) * 0.2;
        double reviewScore = Math.log1p(ratingCount != null ? ratingCount : 0) * 0.1;
        
        return conversationScore + ratingScore + engagementScore + reviewScore;
    }
    
    private String generateName(String prompt) {
        // Simple name generation - in production would use NLG
        String[] nameTemplates = {
            "Luna", "Atlas", "Nova", "Orion", "Aria", "Phoenix", "Sage", "Echo"
        };
        return nameTemplates[new Random().nextInt(nameTemplates.length)];
    }
    
    private String generateDescription(String prompt) {
        // Generate a character description based on the prompt
        return "A unique character inspired by: " + prompt;
    }
    
    private Character.Gender parseGender(String gender) {
        if (gender == null) return Character.Gender.OTHER;
        try {
            return Character.Gender.valueOf(gender.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Character.Gender.OTHER;
        }
    }
    
    private int generateAge(String prompt) {
        // Generate appropriate age based on context
        return 25 + new Random().nextInt(20);
    }
    
    private String generateBackstory(String prompt) {
        // Generate backstory - simplified version
        return "Born from the imagination sparked by '" + prompt + "', this character has a unique story to tell...";
    }
    
    private String determineDominantTrait(CharacterPersonality personality) {
        Map<String, Double> traits = new HashMap<>();
        traits.put("Openness", personality.getOpenness());
        traits.put("Conscientiousness", personality.getConscientiousness());
        traits.put("Extraversion", personality.getExtraversion());
        traits.put("Agreeableness", personality.getAgreeableness());
        traits.put("Neuroticism", personality.getNeuroticism());
        
        return traits.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Balanced");
    }
    
    private String generateBehaviorNotes(String prompt) {
        return "Character behavior influenced by: " + prompt;
    }
    
    private int calculateRecommendationScore(Character character, Set<String> preferredCategories, Map<String, Integer> tagFrequency) {
        int score = 0;
        
        if (preferredCategories.contains(character.getCategory())) {
            score += 10;
        }
        
        for (var tag : character.getTags()) {
            score += tagFrequency.getOrDefault(tag.getName(), 0);
        }
        
        score += (int) (character.getPopularityScore() * 5);
        
        return score;
    }
}