package com.virtualcompanion.user.controller;

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
