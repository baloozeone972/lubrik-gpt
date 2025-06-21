package com.virtualcompanion.userservice.controller;

class UserControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    private String authToken;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
                .email("test@example.com")
                .username("testuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("Test")
                .lastName("User")
                .age(25)
                .phoneNumber("+1234567890")
                .locale("en_US")
                .subscriptionLevel(User.SubscriptionLevel.FREE)
                .isActive(true)
                .emailVerified(true)
                .twoFactorEnabled(false)
                .roles(Set.of("ROLE_USER"))
                .build();
        
        testUser = userRepository.save(testUser);
        
        // Generate auth token
        authToken = jwtTokenProvider.generateAccessToken(testUser);
    }
    
    @Test
    void register_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .username("newuser")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .age(30)
                .phoneNumber("+9876543210")
                .locale("en_US")
                .jurisdiction("US")
                .build();
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.username").value(request.getUsername()));
    }
    
    @Test
    void login_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }
    
    @Test
    void getCurrentUser_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()));
    }
    
    @Test
    void updateUser_Success() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .phoneNumber("+1111111111")
                .build();
        
        mockMvc.perform(put("/api/v1/users/" + testUser.getId())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId().toString()));
    }
    
    @Test
    void getUserPreferences_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + testUser.getId() + "/preferences")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").exists())
                .andExpect(jsonPath("$.language").exists());
    }
    
    @Test
    void updatePassword_Success() throws Exception {
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .currentPassword("password123")
                .newPassword("newPassword123")
                .build();
        
        mockMvc.perform(post("/api/v1/users/" + testUser.getId() + "/password")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
    
    @Test
    void unauthorizedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void accessOtherUserProfile_Returns403() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/v1/users/" + otherUserId)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isForbidden());
    }
}
