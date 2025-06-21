package com.virtualcompanion.characterservice.service;

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
