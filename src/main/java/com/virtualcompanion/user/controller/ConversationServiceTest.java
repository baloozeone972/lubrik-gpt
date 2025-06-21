package com.virtualcompanion.user.controller;

class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ResponseGenerator responseGenerator;

    @Mock
    private WebSocketSessionManager sessionManager;

    @InjectMocks
    private ConversationService conversationService;

    private Conversation testConversation;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        UUID conversationId = UUID.randomUUID();
        
        testConversation = Conversation.builder()
                .id(conversationId)
                .userId(UUID.randomUUID())
                .characterId(UUID.randomUUID())
                .title("Test Conversation")
                .messageCount(0)
                .build();

        testMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .content("Hello!")
                .role(MessageRole.USER)
                .build();
    }

    @Test
    @DisplayName("Should create conversation successfully")
    void createConversation_Success() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest();
        request.setCharacterId(UUID.randomUUID());
        request.setInitialMessage("Hello!");

        when(conversationRepository.save(any())).thenReturn(testConversation);
        when(messageRepository.save(any())).thenReturn(testMessage);
        when(responseGenerator.generateResponse(any(), any()))
                .thenReturn(Mono.just("Hi there!"));

        // When
        ConversationResponse response = conversationService.createConversation(request, "user-id");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo(testConversation.getTitle());
        verify(conversationRepository).save(any());
        verify(messageRepository, times(2)).save(any()); // User message + AI response
    }

    @Test
    @DisplayName("Should handle message streaming")
    void sendMessage_Streaming() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Tell me a story");
        request.setStream(true);

        when(conversationRepository.findById(any())).thenReturn(Optional.of(testConversation));
        when(messageRepository.save(any())).thenReturn(testMessage);
        when(responseGenerator.streamResponse(any(), any()))
                .thenReturn(Mono.just("Once upon a time..."));

        // When
        Mono<MessageResponse> responseMono = conversationService.sendMessage(
                testConversation.getId(), request, "user-id");

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getContent()).isNotEmpty();
                })
                .verifyComplete();

        verify(sessionManager).sendToSession(anyString(), any());
    }

    @Test
    @DisplayName("Should enforce rate limiting")
    void sendMessage_RateLimited() {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Message");

        when(conversationRepository.findById(any())).thenReturn(Optional.of(testConversation));

        // Simulate sending multiple messages quickly
        for (int i = 0; i < 10; i++) {
            conversationService.sendMessage(testConversation.getId(), request, "user-id");
        }

        // Then
        verify(messageRepository, atMost(5)).save(any()); // Rate limit of 5 messages
    }

    @Test
    @DisplayName("Should handle conversation context")
    void sendMessage_WithContext() {
        // Given
        List<Message> history = Arrays.asList(
                createMessage("Hello", MessageRole.USER),
                createMessage("Hi there!", MessageRole.ASSISTANT),
                createMessage("How are you?", MessageRole.USER)
        );

        when(conversationRepository.findById(any())).thenReturn(Optional.of(testConversation));
        when(messageRepository.findRecentMessages(any(), anyInt())).thenReturn(history);
        when(messageRepository.save(any())).thenReturn(testMessage);
        when(responseGenerator.generateResponse(any(), any()))
                .thenReturn(Mono.just("I'm doing well, thanks!"));

        SendMessageRequest request = new SendMessageRequest();
        request.setContent("Great!");

        // When
        MessageResponse response = conversationService.sendMessage(
                testConversation.getId(), request, "user-id").block();

        // Then
        assertThat(response).isNotNull();
        verify(responseGenerator).generateResponse(
                argThat(ctx -> ctx.getMessages().size() == history.size()),
                any()
        );
    }

    private Message createMessage(String content, MessageRole role) {
        return Message.builder()
                .id(UUID.randomUUID())
                .content(content)
                .role(role)
                .build();
    }
}
