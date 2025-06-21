// ==================== USER SERVICE TESTS ====================

// UserService/src/test/java/com/virtualcompanion/user/controller/UserControllerTest.java
package com.virtualcompanion.user.controller;

import com.virtualcompanion.user.dto.*;
import com.virtualcompanion.user.service.UserService;
import com.virtualcompanion.user.exception.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserResponse mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .displayName("Test User")
                .avatarUrl("https://example.com/avatar.jpg")
                .build();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void registerUser_Success() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("SecurePass123!");
        request.setDisplayName("New User");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken("jwt-token");
        authResponse.setUser(mockUser);

        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value(mockUser.getEmail()));

        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should fail registration with invalid email")
    void registerUser_InvalidEmail() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("SecurePass123!");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void loginUser_Success() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken("jwt-token");
        authResponse.setUser(mockUser);

        when(userService.login(any(LoginRequest.class)))
                .thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.id").value(userId.toString()));
    }

    @Test
    @WithMockUser(username = "test-user-id")
    @DisplayName("Should get user profile successfully")
    void getUserProfile_Success() throws Exception {
        // Given
        when(userService.getUser(anyString())).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(get("/api/v1/users/profile")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "test-user-id"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should handle user not found exception")
    void getUserProfile_NotFound() throws Exception {
        // Given
        when(userService.getUser(anyString()))
                .thenThrow(new UserNotFoundException("User not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/users/profile")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "unknown-user"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should update user profile successfully")
    void updateUserProfile_Success() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Updated Name");
        request.setBio("New bio");

        when(userService.updateProfile(anyString(), any(UpdateProfileRequest.class)))
                .thenReturn(mockUser);

        // When & Then
        mockMvc.perform(put("/api/v1/users/profile")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "test-user-id")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    @WithMockUser
    @DisplayName("Should delete user account successfully")
    void deleteUser_Success() throws Exception {
        // Given
        doNothing().when(userService).deleteUser(anyString());

        // When & Then
        mockMvc.perform(delete("/api/v1/users/profile")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "test-user-id"))))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(anyString());
    }
}

// UserService/src/test/java/com/virtualcompanion/user/service/UserServiceTest.java
package com.virtualcompanion.user.service;

import com.virtualcompanion.user.dto.*;
import com.virtualcompanion.user.entity.User;
import com.virtualcompanion.user.entity.UserProfile;
import com.virtualcompanion.user.exception.*;
import com.virtualcompanion.user.repository.UserRepository;
import com.virtualcompanion.user.repository.UserProfileRepository;
import com.virtualcompanion.user.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserProfile testProfile;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .password("encoded-password")
                .enabled(true)
                .build();

        testProfile = UserProfile.builder()
                .id(userId)
                .userId(userId)
                .displayName("Test User")
                .bio("Test bio")
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void register_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setDisplayName("New User");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(profileRepository.save(any(UserProfile.class))).thenReturn(testProfile);
        when(jwtTokenProvider.generateToken(any())).thenReturn("jwt-token");

        // When
        AuthResponse response = userService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo(testUser.getEmail());

        verify(userRepository).save(any(User.class));
        verify(profileRepository).save(any(UserProfile.class));
        verify(emailService).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void register_EmailExists() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login user successfully")
    void login_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(profileRepository.findById(any())).thenReturn(Optional.of(testProfile));
        when(jwtTokenProvider.generateToken(any())).thenReturn("jwt-token");

        // When
        AuthResponse response = userService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should throw exception for invalid credentials")
    void login_InvalidCredentials() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Should update user profile successfully")
    void updateProfile_Success() {
        // Given
        String userId = testUser.getId().toString();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Updated Name");
        request.setBio("Updated bio");

        when(profileRepository.findById(any())).thenReturn(Optional.of(testProfile));
        when(profileRepository.save(any())).thenReturn(testProfile);

        // When
        UserResponse response = userService.updateProfile(userId, request);

        // Then
        assertThat(response).isNotNull();
        verify(profileRepository).save(argThat(profile -> 
            profile.getDisplayName().equals("Updated Name") &&
            profile.getBio().equals("Updated bio")
        ));
    }

    @Test
    @DisplayName("Should handle concurrent profile updates")
    void updateProfile_ConcurrentUpdates() {
        // Given
        String userId = testUser.getId().toString();
        UpdateProfileRequest request1 = new UpdateProfileRequest();
        request1.setDisplayName("Name 1");
        
        UpdateProfileRequest request2 = new UpdateProfileRequest();
        request2.setDisplayName("Name 2");

        when(profileRepository.findById(any())).thenReturn(Optional.of(testProfile));
        when(profileRepository.save(any())).thenReturn(testProfile);

        // When - Simulate concurrent updates
        Thread thread1 = new Thread(() -> userService.updateProfile(userId, request1));
        Thread thread2 = new Thread(() -> userService.updateProfile(userId, request2));

        thread1.start();
        thread2.start();

        // Then
        assertThatCode(() -> {
            thread1.join();
            thread2.join();
        }).doesNotThrowAnyException();

        verify(profileRepository, times(2)).save(any());
    }
}

// ==================== CHARACTER SERVICE TESTS ====================

// CharacterService/src/test/java/com/virtualcompanion/character/service/CharacterServiceTest.java
package com.virtualcompanion.character.service;

import com.virtualcompanion.character.dto.*;
import com.virtualcompanion.character.entity.Character;
import com.virtualcompanion.character.entity.CharacterPersonality;
import com.virtualcompanion.character.repository.CharacterRepository;
import com.virtualcompanion.character.ai.PersonalityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

// ==================== CONVERSATION SERVICE TESTS ====================

// ConversationService/src/test/java/com/virtualcompanion/conversation/service/ConversationServiceTest.java
package com.virtualcompanion.conversation.service;

import com.virtualcompanion.conversation.dto.*;
import com.virtualcompanion.conversation.entity.Conversation;
import com.virtualcompanion.conversation.entity.Message;
import com.virtualcompanion.conversation.repository.ConversationRepository;
import com.virtualcompanion.conversation.repository.MessageRepository;
import com.virtualcompanion.conversation.ai.ResponseGenerator;
import com.virtualcompanion.conversation.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

// ==================== BILLING SERVICE TESTS ====================

// BillingService/src/test/java/com/virtualcompanion/billing/service/SubscriptionServiceTest.java
package com.virtualcompanion.billing.service;

import com.virtualcompanion.billing.dto.*;
import com.virtualcompanion.billing.entity.Subscription;
import com.virtualcompanion.billing.entity.SubscriptionTier;
import com.virtualcompanion.billing.repository.SubscriptionRepository;
import com.virtualcompanion.billing.stripe.StripeService;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private StripeService stripeService;

    @Mock
    private UsageTrackingService usageService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Subscription testSubscription;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        testSubscription = Subscription.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tier(SubscriptionTier.PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDateTime.now())
                .currentPeriodEnd(LocalDateTime.now().plusMonths(1))
                .stripeSubscriptionId("sub_123")
                .build();
    }

    @Test
    @DisplayName("Should create subscription successfully")
    void createSubscription_Success() throws Exception {
        // Given
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setTier(SubscriptionTier.PREMIUM);
        request.setPaymentMethodId("pm_123");

        Customer mockCustomer = mock(Customer.class);
        when(mockCustomer.getId()).thenReturn("cus_123");
        
        com.stripe.model.Subscription mockStripeSubscription = mock(com.stripe.model.Subscription.class);
        when(mockStripeSubscription.getId()).thenReturn("sub_123");

        when(stripeService.createCustomer(any())).thenReturn(mockCustomer);
        when(stripeService.createSubscription(any())).thenReturn(mockStripeSubscription);
        when(subscriptionRepository.save(any())).thenReturn(testSubscription);

        // When
        SubscriptionResponse response = subscriptionService.createSubscription(request, userId.toString());

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTier()).isEqualTo(SubscriptionTier.PREMIUM);
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        
        verify(stripeService).createCustomer(any());
        verify(stripeService).createSubscription(any());
        verify(subscriptionRepository).save(any());
    }

    @Test
    @DisplayName("Should handle subscription cancellation")
    void cancelSubscription_Success() throws Exception {
        // Given
        when(subscriptionRepository.findByUserIdAndStatus(any(), any()))
                .thenReturn(Optional.of(testSubscription));
        
        com.stripe.model.Subscription cancelledSub = mock(com.stripe.model.Subscription.class);
        when(stripeService.cancelSubscription(anyString())).thenReturn(cancelledSub);

        // When
        subscriptionService.cancelSubscription(userId.toString());

        // Then
        verify(stripeService).cancelSubscription("sub_123");
        verify(subscriptionRepository).save(argThat(sub -> 
            sub.getStatus() == SubscriptionStatus.CANCELLED
        ));
    }

    @Test
    @DisplayName("Should track usage correctly")
    void trackUsage_Success() {
        // Given
        when(subscriptionRepository.findByUserIdAndStatus(any(), any()))
                .thenReturn(Optional.of(testSubscription));

        UsageEvent event = UsageEvent.builder()
                .userId(userId)
                .type(UsageType.MESSAGE)
                .amount(1)
                .build();

        // When
        subscriptionService.trackUsage(event);

        // Then
        verify(usageService).recordUsage(event);
    }

    @Test
    @DisplayName("Should handle payment failure")
    void handlePaymentFailure() {
        // Given
        when(subscriptionRepository.findByStripeSubscriptionId(anyString()))
                .thenReturn(Optional.of(testSubscription));

        // When
        subscriptionService.handlePaymentFailure("sub_123");

        // Then
        verify(subscriptionRepository).save(argThat(sub -> 
            sub.getStatus() == SubscriptionStatus.PAST_DUE
        ));
    }

    @Test
    @DisplayName("Should enforce usage limits")
    void enforceUsageLimits() {
        // Given
        testSubscription.setTier(SubscriptionTier.FREE);
        
        when(subscriptionRepository.findByUserIdAndStatus(any(), any()))
                .thenReturn(Optional.of(testSubscription));
        when(usageService.getCurrentUsage(any(), any()))
                .thenReturn(100L); // At limit

        // When & Then
        assertThatThrownBy(() -> 
            subscriptionService.checkUsageLimit(userId.toString(), UsageType.MESSAGE)
        )
        .isInstanceOf(UsageLimitExceededException.class)
        .hasMessage("Usage limit exceeded for MESSAGE");
    }
}

// ==================== INTEGRATION TESTS ====================

// src/test/java/com/virtualcompanion/integration/EndToEndTest.java
package com.virtualcompanion.integration;

import com.virtualcompanion.VirtualCompanionApplication;
import com.virtualcompanion.user.dto.*;
import com.virtualcompanion.character.dto.*;
import com.virtualcompanion.conversation.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = VirtualCompanionApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
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

// ==================== PERFORMANCE TESTS ====================

// src/test/java/com/virtualcompanion/performance/LoadTest.java
package com.virtualcompanion.performance;

import com.virtualcompanion.conversation.service.ConversationService;
import com.virtualcompanion.conversation.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LoadTest {

    @Autowired
    private ConversationService conversationService;

    @Test
    @DisplayName("Should handle concurrent message processing")
    void testConcurrentMessages() throws Exception {
        // Given
        int numberOfThreads = 10;
        int messagesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ConcurrentLinkedQueue<Exception> exceptions = new ConcurrentLinkedQueue<>();

        // When
        IntStream.range(0, numberOfThreads).forEach(i -> {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < messagesPerThread; j++) {
                        SendMessageRequest request = new SendMessageRequest();
                        request.setContent("Concurrent message " + j);
                        // Process message
                        conversationService.processMessage(request);
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        });

        // Then
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(exceptions).isEmpty();
    }

    @Test
    @DisplayName("Should maintain response time under load")
    void testResponseTimeUnderLoad() throws Exception {
        // Given
        int requests = 1000;
        long maxResponseTime = 200; // milliseconds
        
        // When
        List<Long> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < requests; i++) {
            long start = System.currentTimeMillis();
            
            SendMessageRequest request = new SendMessageRequest();
            request.setContent("Performance test message");
            conversationService.processMessage(request);
            
            long responseTime = System.currentTimeMillis() - start;
            responseTimes.add(responseTime);
        }

        // Then
        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
                
        long p95ResponseTime = responseTimes.stream()
                .sorted()
                .skip((long) (requests * 0.95))
                .findFirst()
                .orElse(0L);

        assertThat(avgResponseTime).isLessThan(maxResponseTime);
        assertThat(p95ResponseTime).isLessThan(maxResponseTime * 2);
    }
}