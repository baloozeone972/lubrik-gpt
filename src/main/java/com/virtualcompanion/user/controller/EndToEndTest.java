package com.virtualcompanion.user.controller;

class EndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String authToken;
    private static String userId;
    private static String characterId;
    private static String conversationId;

    @Test
    @Order(1)
    @DisplayName("Complete user journey - Register")
    void userJourney_Register() throws Exception {
        // Register new user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("e2e.test@example.com");
        registerRequest.setPassword("SecurePassword123!");
        registerRequest.setDisplayName("E2E Test User");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("e2e.test@example.com"))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                AuthResponse.class
        );
        
        authToken = authResponse.getToken();
        userId = authResponse.getUser().getId().toString();
    }

    @Test
    @Order(2)
    @DisplayName("Complete user journey - Create Character")
    void userJourney_CreateCharacter() throws Exception {
        CreateCharacterRequest request = new CreateCharacterRequest();
        request.setName("E2E Test Character");
        request.setDescription("A character for E2E testing");
        request.setTraits(Arrays.asList("helpful", "friendly"));

        MvcResult result = mockMvc.perform(post("/api/v1/characters")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("E2E Test Character"))
                .andReturn();

        CharacterResponse characterResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                CharacterResponse.class
        );
        
        characterId = characterResponse.getId().toString();
    }

    @Test
    @Order(3)
    @DisplayName("Complete user journey - Start Conversation")
    void userJourney_StartConversation() throws Exception {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setCharacterId(UUID.fromString(characterId));
        request.setInitialMessage("Hello, let's test the conversation!");

        MvcResult result = mockMvc.perform(post("/api/v1/conversations")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.characterId").value(characterId))
                .andReturn();

        ConversationResponse conversationResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), 
                ConversationResponse.class
        );
        
        conversationId = conversationResponse.getId().toString();
    }

    @Test
    @Order(4)
    @DisplayName("Complete user journey - Send Messages")
    void userJourney_SendMessages() throws Exception {
        // Send multiple messages
        for (int i = 0; i < 3; i++) {
            SendMessageRequest request = new SendMessageRequest();
            request.setContent("Test message " + i);

            mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").exists());

            Thread.sleep(100); // Avoid rate limiting
        }

        // Verify conversation history
        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(7)); // Initial + 3 user + 3 AI
    }

    @Test
    @Order(5)
    @DisplayName("Complete user journey - Check Usage")
    void userJourney_CheckUsage() throws Exception {
        mockMvc.perform(get("/api/v1/billing/usage")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageCount").value(4))
                .andExpect(jsonPath("$.characterCount").value(1));
    }
}
