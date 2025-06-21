// CharacterController.java
package com.virtualcompanion.characterservice.controller;

import com.virtualcompanion.characterservice.dto.*;
import com.virtualcompanion.characterservice.service.CharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/characters")
@RequiredArgsConstructor
@Tag(name = "Characters", description = "Character management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class CharacterController {
    
    private final CharacterService characterService;
    
    @PostMapping
    @Operation(summary = "Create a new character")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CharacterResponse> createCharacter(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateCharacterRequest request) {
        CharacterResponse character = characterService.createCharacter(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(character);
    }
    
    @GetMapping("/{characterId}")
    @Operation(summary = "Get character by ID")
    public ResponseEntity<CharacterResponse> getCharacter(@PathVariable UUID characterId) {
        CharacterResponse character = characterService.getCharacterById(characterId);
        return ResponseEntity.ok(character);
    }
    
    @PutMapping("/{characterId}")
    @Operation(summary = "Update character")
    @PreAuthorize("@characterService.isOwner(#userId, #characterId)")
    public ResponseEntity<CharacterResponse> updateCharacter(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID characterId,
            @Valid @RequestBody UpdateCharacterRequest request) {
        CharacterResponse character = characterService.updateCharacter(userId, characterId, request);
        return ResponseEntity.ok(character);
    }
    
    @DeleteMapping("/{characterId}")
    @Operation(summary = "Delete character")
    @PreAuthorize("@characterService.isOwner(#userId, #characterId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCharacter(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID characterId) {
        characterService.deleteCharacter(userId, characterId);
    }
    
    @GetMapping("/my-characters")
    @Operation(summary = "Get user's characters")
    public ResponseEntity<Page<CharacterResponse>> getMyCharacters(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<CharacterResponse> characters = characterService.getUserCharacters(userId, pageRequest);
        return ResponseEntity.ok(characters);
    }
    
    @GetMapping("/public")
    @Operation(summary = "Get public characters")
    public ResponseEntity<Page<CharacterResponse>> getPublicCharacters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "popularityScore") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<CharacterResponse> characters = characterService.getPublicCharacters(pageRequest);
        return ResponseEntity.ok(characters);
    }
    
    @PostMapping("/search")
    @Operation(summary = "Search characters")
    public ResponseEntity<CharacterSearchResponse> searchCharacters(
            @Valid @RequestBody CharacterSearchRequest request) {
        CharacterSearchResponse response = characterService.searchCharacters(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{characterId}/rate")
    @Operation(summary = "Rate a character")
    public ResponseEntity<CharacterResponse> rateCharacter(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID characterId,
            @Valid @RequestBody RateCharacterRequest request) {
        CharacterResponse character = characterService.rateCharacter(userId, characterId, request);
        return ResponseEntity.ok(character);
    }
    
    @GetMapping("/{characterId}/ratings")
    @Operation(summary = "Get character ratings")
    public ResponseEntity<Page<CharacterRatingResponse>> getCharacterRatings(
            @PathVariable UUID characterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<CharacterRatingResponse> ratings = characterService.getCharacterRatings(characterId, pageRequest);
        return ResponseEntity.ok(ratings);
    }
    
    @GetMapping("/{characterId}/statistics")
    @Operation(summary = "Get character statistics")
    public ResponseEntity<CharacterStatisticsResponse> getCharacterStatistics(
            @PathVariable UUID characterId) {
        CharacterStatisticsResponse statistics = characterService.getCharacterStatistics(characterId);
        return ResponseEntity.ok(statistics);
    }
    
    @PostMapping("/{characterId}/favorite")
    @Operation(summary = "Toggle character favorite status")
    public ResponseEntity<CharacterResponse> toggleFavorite(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID characterId) {
        CharacterResponse character = characterService.toggleFavorite(userId, characterId);
        return ResponseEntity.ok(character);
    }
    
    @GetMapping("/favorites")
    @Operation(summary = "Get user's favorite characters")
    public ResponseEntity<List<CharacterResponse>> getFavorites(
            @AuthenticationPrincipal UUID userId) {
        List<CharacterResponse> favorites = characterService.getUserFavorites(userId);
        return ResponseEntity.ok(favorites);
    }
    
    @PostMapping("/generate")
    @Operation(summary = "Generate a character using AI")
    public ResponseEntity<CharacterResponse> generateCharacter(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody GenerateCharacterRequest request) {
        CharacterResponse character = characterService.generateCharacter(userId, request);
        return ResponseEntity.ok(character);
    }
    
    @PostMapping("/{characterId}/images")
    @Operation(summary = "Upload character image")
    @PreAuthorize("@characterService.isOwner(#userId, #characterId)")
    public ResponseEntity<CharacterImageUploadResponse> uploadImage(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID characterId,
            @RequestParam("file") MultipartFile file) {
        CharacterImageUploadResponse response = characterService.uploadCharacterImage(userId, characterId, file);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{characterId}/images/{imageId}")
    @Operation(summary = "Delete character image")
    @PreAuthorize("@characterService.isOwner(#userId, #characterId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID characterId,
            @PathVariable UUID imageId) {
        characterService.deleteCharacterImage(userId, characterId, imageId);
    }
    
    @GetMapping("/{characterId}/images")
    @Operation(summary = "Get character images")
    public ResponseEntity<List<CharacterImageUploadResponse>> getCharacterImages(
            @PathVariable UUID characterId) {
        List<CharacterImageUploadResponse> images = characterService.getCharacterImages(characterId);
        return ResponseEntity.ok(images);
    }
    
    @PostMapping("/{characterId}/interact")
    @Operation(summary = "Record character interaction")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordInteraction(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID characterId) {
        characterService.recordInteraction(userId, characterId);
    }
    
    @GetMapping("/recommended")
    @Operation(summary = "Get recommended characters for user")
    public ResponseEntity<List<CharacterResponse>> getRecommended(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "10") int limit) {
        List<CharacterResponse> recommendations = characterService.getRecommendedCharacters(userId, limit);
        return ResponseEntity.ok(recommendations);
    }
    
    @GetMapping("/trending")
    @Operation(summary = "Get trending characters")
    public ResponseEntity<List<CharacterResponse>> getTrending(
            @RequestParam(defaultValue = "10") int limit) {
        List<CharacterResponse> trending = characterService.getTrendingCharacters(limit);
        return ResponseEntity.ok(trending);
    }
    
    @GetMapping("/categories")
    @Operation(summary = "Get available character categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = List.of(
            "FANTASY", "REALISTIC", "ANIME", "HISTORICAL", 
            "FUTURISTIC", "CELEBRITY", "CUSTOM"
        );
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/popular-tags")
    @Operation(summary = "Get popular character tags")
    public ResponseEntity<List<TagResponse>> getPopularTags(
            @RequestParam(defaultValue = "20") int limit) {
        // This would be implemented in the service
        List<TagResponse> tags = characterService.getPopularTags(limit);
        return ResponseEntity.ok(tags);
    }
}