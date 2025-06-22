package com.virtualcompanion.characterservice.dto;

public class CharacterSearchRequest {

    private String query;
    private List<String> categories;
    private List<String> tags;
    private String gender;
    private Integer minAge;
    private Integer maxAge;
    private Double minRating;
    private Boolean isPublic;
    private Boolean isNsfw;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer size;
}
