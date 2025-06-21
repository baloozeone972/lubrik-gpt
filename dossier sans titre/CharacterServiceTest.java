package com.virtualcompanion.character.service;

class CharacterServiceTest {

    @Mock
    private CharacterRepository characterRepository;

    @Mock
    private PersonalityGenerator personalityGenerator;

    @Mock
    private CharacterValidator validator;

    @InjectMocks
    private CharacterService characterService;

    private Character testCharacter;
    private CharacterPersonality testPersonality;

    @BeforeEach
    void setUp() {
        testPersonality = CharacterPersonality.builder()
                .traits(Arrays.asList("friendly", "intelligent"))
                .interests(Arrays.asList("technology", "music"))
                .speakingStyle("casual")
                .backstory("A helpful AI companion")
                .build();

        testCharacter = Character.builder()
                .id(UUID.randomUUID())
                .name("Test Character")
                .description("A test character")
                .personality(testPersonality)
                .creatorId(UUID.randomUUID())
                .isPublic(true)
                .build();
    }

    @Test
    @DisplayName("Should create character successfully")
    void createCharacter_Success() {
        // Given
        CreateCharacterRequest request = new CreateCharacterRequest();
        request.setName("New Character");
        request.setDescription("A new character");
        request.setTraits(Arrays.asList("kind", "smart"));

        when(validator.validate(any())).thenReturn(Mono.just(true));
        when(personalityGenerator.generate(any())).thenReturn(Mono.just(testPersonality));
        when(characterRepository.save(any())).thenReturn(testCharacter);

        // When
        CharacterResponse response = characterService.createCharacter(request, "user-id");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(testCharacter.getName());
        verify(characterRepository).save(any());
    }

    @Test
    @DisplayName("Should find public characters")
    void findPublicCharacters_Success() {
        // Given
        List<Character> characters = Arrays.asList(testCharacter);
        Page<Character> page = new PageImpl<>(characters);
        
        when(characterRepository.findByIsPublicTrue(any())).thenReturn(page);

        // When
        Page<CharacterResponse> response = characterService.findPublicCharacters(PageRequest.of(0, 10));

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo(testCharacter.getName());
    }

    @Test
    @DisplayName("Should validate character limits")
    void createCharacter_ExceedsLimits() {
        // Given
        String longName = "a".repeat(256); // Exceeds typical name limit
        CreateCharacterRequest request = new CreateCharacterRequest();
        request.setName(longName);

        when(validator.validate(any())).thenReturn(Mono.error(new ValidationException("Name too long")));

        // When & Then
        assertThatThrownBy(() -> characterService.createCharacter(request, "user-id"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Name too long");

        verify(characterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle character search with filters")
    void searchCharacters_WithFilters() {
        // Given
        CharacterSearchRequest searchRequest = new CharacterSearchRequest();
        searchRequest.setQuery("test");
        searchRequest.setTags(Arrays.asList("AI", "friendly"));
        searchRequest.setMinRating(4.0);

        List<Character> results = Arrays.asList(testCharacter);
        when(characterRepository.searchCharacters(any(), any(), anyDouble(), any()))
                .thenReturn(new PageImpl<>(results));

        // When
        Page<CharacterResponse> response = characterService.searchCharacters(searchRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        verify(characterRepository).searchCharacters(
                eq("test"), 
                eq(Arrays.asList("AI", "friendly")), 
                eq(4.0), 
                any()
        );
    }
}
