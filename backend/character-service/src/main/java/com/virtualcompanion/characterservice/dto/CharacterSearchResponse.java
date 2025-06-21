package com.virtualcompanion.characterservice.dto;

public class CharacterSearchResponse {
    
    private List<CharacterResponse> characters;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    private Map<String, Map<String, Long>> facets;
}
