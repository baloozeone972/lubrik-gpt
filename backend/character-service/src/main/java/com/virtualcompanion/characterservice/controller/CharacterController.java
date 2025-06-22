package com.virtualcompanion.characterservice.controller;

public class CharacterController {

    private final CharacterService characterService;
    private final CharacterSearchService searchService;
    private final PersonalityEngineService personalityEngine;
    private final CharacterImageService imageService;
    private final CharacterVoiceService voiceService;
    private final CharacterAnalyticsService analyticsService;

    // ========== CRUD Operations ==========

    @PostMapping
    @Operation(summary = "Créer un nouveau personnage")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CREATOR')")
    public Mono<ResponseEntity<CharacterResponseDto>> createCharacter(
            @Valid @RequestBody CharacterCreateDto createDto,
            @CurrentUser UserPrincipal currentUser) {

        log.info("Creating new character by user: {}", currentUser.getId());

        return characterService.createCharacter(createDto, currentUser.getId())
                .map(character -> ResponseEntity.status(HttpStatus.CREATED).body(character))
                .doOnSuccess(result -> log.info("Character created: {}", result.getBody().getId()))
                .doOnError(error -> log.error("Error creating character: ", error));
    }

    @GetMapping("/{characterId}")
    @Operation(summary = "Obtenir un personnage par ID")
    public Mono<CharacterResponseDto> getCharacter(
            @PathVariable String characterId,
            @CurrentUser UserPrincipal currentUser) {

        return characterService.getCharacterById(characterId, currentUser.getId())
                .doOnSuccess(character -> analyticsService.trackView(characterId, currentUser.getId()));
    }

    @PutMapping("/{characterId}")
    @Operation(summary = "Mettre à jour un personnage")
    @PreAuthorize("@characterService.isOwner(#characterId, #currentUser.id) or hasRole('ADMIN')")
    public Mono<CharacterResponseDto> updateCharacter(
            @PathVariable String characterId,
            @Valid @RequestBody CharacterUpdateDto updateDto,
            @CurrentUser UserPrincipal currentUser) {

        return characterService.updateCharacter(characterId, updateDto, currentUser.getId())
                .doOnSuccess(result -> log.info("Character updated: {}", characterId));
    }

    @DeleteMapping("/{characterId}")
    @Operation(summary = "Supprimer un personnage")
    @PreAuthorize("@characterService.isOwner(#characterId, #currentUser.id) or hasRole('ADMIN')")
    public Mono<Void> deleteCharacter(
            @PathVariable String characterId,
            @CurrentUser UserPrincipal currentUser) {

        return characterService.deleteCharacter(characterId, currentUser.getId())
                .doOnSuccess(v -> log.info("Character deleted: {}", characterId));
    }

    // ========== Search & Discovery ==========

    @GetMapping("/search")
    @Operation(summary = "Rechercher des personnages")
    public Mono<PageResponse<CharacterSearchResultDto>> searchCharacters(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String accessLevel,
            @RequestParam(defaultValue = "POPULARITY") String sortBy,
            Pageable pageable,
            @CurrentUser UserPrincipal currentUser) {

        CharacterSearchCriteria criteria = CharacterSearchCriteria.builder()
                .query(query)
                .categories(categories)
                .tags(tags)
                .language(language)
                .minAge(minAge)
                .maxAge(maxAge)
                .gender(gender)
                .accessLevel(accessLevel)
                .sortBy(sortBy)
                .userId(currentUser.getId())
                .userSubscriptionLevel(currentUser.getSubscriptionLevel())
                .build();

        return searchService.searchCharacters(criteria, pageable);
    }

    @GetMapping("/trending")
    @Operation(summary = "Obtenir les personnages tendance")
    public Flux<CharacterTrendingDto> getTrendingCharacters(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {

        return characterService.getTrendingCharacters(days, limit)
                .take(limit);
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Obtenir des recommandations personnalisées")
    public Flux<CharacterRecommendationDto> getRecommendations(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(defaultValue = "10") int limit) {

        return characterService.getPersonalizedRecommendations(currentUser.getId(), limit)
                .take(limit);
    }

    // ========== Personality & Behavior ==========

    @PostMapping("/{characterId}/personality/analyze")
    @Operation(summary = "Analyser et générer une personnalité")
    @PreAuthorize("@characterService.isOwner(#characterId, #currentUser.id) or hasRole('ADMIN')")
    public Mono<PersonalityAnalysisDto> analyzePersonality(
            @PathVariable String characterId,
            @RequestBody PersonalityInputDto input,
            @CurrentUser UserPrincipal currentUser) {

        return personalityEngine.analyzeAndGeneratePersonality(characterId, input)
                .timeout(Duration.ofSeconds(30));
    }

    @GetMapping("/{characterId}/personality/traits")
    @Operation(summary = "Obtenir les traits de personnalité")
    public Mono<PersonalityTraitsDto> getPersonalityTraits(@PathVariable String characterId) {
        return characterService.getPersonalityTraits(characterId);
    }

    @PostMapping("/{characterId}/personality/test")
    @Operation(summary = "Tester le comportement du personnage")
    public Mono<PersonalityTestResultDto> testPersonalityBehavior(
            @PathVariable String characterId,
            @RequestBody PersonalityTestDto testDto) {

        return personalityEngine.testPersonalityResponse(characterId, testDto);
    }

    // ========== Media Management ==========

    @PostMapping("/{characterId}/images")
    @Operation(summary = "Uploader une image de personnage")
    @PreAuthorize("@characterService.isOwner(#characterId, #currentUser.id) or hasRole('ADMIN')")
    public Mono<CharacterImageDto> uploadImage(
            @PathVariable String characterId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String type,
            @CurrentUser UserPrincipal currentUser) {

        return imageService.uploadCharacterImage(characterId, file, type);
    }

    @PostMapping("/{characterId}/images/generate")
    @Operation(summary = "Générer une image avec IA")
    @PreAuthorize("@characterService.isOwner(#characterId, #currentUser.id) or hasRole('ADMIN')")
    public Mono<CharacterImageDto> generateImage(
            @PathVariable String characterId,
            @RequestBody ImageGenerationRequestDto request,
            @CurrentUser UserPrincipal currentUser) {

        return imageService.generateCharacterImage(characterId, request)
                .timeout(Duration.ofMinutes(2));
    }

    @GetMapping("/{characterId}/images")
    @Operation(summary = "Lister les images du personnage")
    public Flux<CharacterImageDto> getCharacterImages(@PathVariable String characterId) {
        return imageService.getCharacterImages(characterId);
    }

    // ========== Voice Configuration ==========

    @PostMapping("/{characterId}/voice/configure")
    @Operation(summary = "Configurer la voix du personnage")
    @PreAuthorize("@characterService.isOwner(#characterId, #currentUser.id) or hasRole('ADMIN')")
    public Mono<VoiceConfigDto> configureVoice(
            @PathVariable String characterId,
            @RequestBody VoiceConfigRequestDto request,
            @CurrentUser UserPrincipal currentUser) {

        return voiceService.configureCharacterVoice(characterId, request);
    }

    @PostMapping("/{characterId}/voice/clone")
    @Operation(summary = "Cloner une voix à partir d'échantillons")
    @PreAuthorize("hasRole('PREMIUM') and @characterService.isOwner(#characterId, #currentUser.id)")
    public Mono<VoiceCloneResultDto> cloneVoice(
            @PathVariable String characterId,
            @RequestParam("samples") List<MultipartFile> audioSamples,
            @CurrentUser UserPrincipal currentUser) {

        return voiceService.cloneVoice(characterId, audioSamples)
                .timeout(Duration.ofMinutes(5));
    }

    @PostMapping("/{characterId}/voice/preview")
    @Operation(summary = "Générer un aperçu audio")
    public Mono<ResponseEntity<byte[]>> previewVoice(
            @PathVariable String characterId,
            @RequestParam(defaultValue = "Bonjour, je suis ravi de faire votre connaissance!") String text) {

        return voiceService.generateVoicePreview(characterId, text)
                .map(audioData -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("audio/mp3"))
                        .body(audioData));
    }

    // ========== User Interactions ==========

    @PostMapping("/{characterId}/favorite")
    @Operation(summary = "Ajouter aux favoris")
    public Mono<Void> addToFavorites(
            @PathVariable String characterId,
            @CurrentUser UserPrincipal currentUser) {

        return characterService.addToFavorites(characterId, currentUser.getId());
    }

    @DeleteMapping("/{characterId}/favorite")
    @Operation(summary = "Retirer des favoris")
    public Mono<Void> removeFromFavorites(
            @PathVariable String characterId,
            @CurrentUser UserPrincipal currentUser) {

        return characterService.removeFromFavorites(characterId, currentUser.getId());
    }

    @PostMapping("/{characterId}/rate")
    @Operation(summary = "Noter un personnage")
    public Mono<CharacterRatingDto> rateCharacter(
            @PathVariable String characterId,
            @RequestBody @Valid RatingDto rating,
            @CurrentUser UserPrincipal currentUser) {

        return characterService.rateCharacter(characterId, currentUser.getId(), rating);
    }

    @GetMapping("/{characterId}/stats")
    @Operation(summary = "Obtenir les statistiques du personnage")
    public Mono<CharacterStatsDto> getCharacterStats(@PathVariable String characterId) {
        return analyticsService.getCharacterStats(characterId);
    }

    // ========== Batch Operations ==========

    @PostMapping("/batch/import")
    @Operation(summary = "Importer plusieurs personnages")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<CharacterImportResultDto> batchImport(
            @RequestParam("file") MultipartFile csvFile,
            @CurrentUser UserPrincipal currentUser) {

        return characterService.batchImportCharacters(csvFile, currentUser.getId())
                .onErrorContinue((error, obj) ->
                        log.error("Error importing character: {}", obj, error));
    }

    @GetMapping("/export")
    @Operation(summary = "Exporter les personnages")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CREATOR')")
    public Mono<ResponseEntity<byte[]>> exportCharacters(
            @RequestParam(required = false) List<String> characterIds,
            @CurrentUser UserPrincipal currentUser) {

        return characterService.exportCharacters(characterIds, currentUser.getId())
                .map(data -> ResponseEntity.ok()
                        .header("Content-Disposition", "attachment; filename=characters.csv")
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .body(data));
    }
}
