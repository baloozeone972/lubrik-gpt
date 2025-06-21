package com.virtualcompanion.userservice.repository;

class UserControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
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
                .password("$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewiYpXO.y/oi/5.2") // "password"
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .subscriptionLevel(SubscriptionLevel.FREE)
                .roles(Set.of(UserRole.USER))
                .build();
        
        testUser = userRepository.save(testUser);
        authToken = jwtTokenProvider.generateAccessToken(testUser);
    }
    
    @Test
    void register_ValidRequest_Returns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .username("newuser")
                .password("SecurePass123!")
                .firstName("New")
                .lastName("User")
                .birthDate(LocalDate.of(1995, 5, 15))
                .acceptedTerms(true)
                .build();
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.email").value("newuser@example.com"));
    }
    
    @Test
    void getProfile_Authenticated_ReturnsProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.data.username").value(testUser.getUsername()));
    }
    
    @Test
    void updateProfile_ValidData_UpdatesSuccessfully() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .phoneNumber("+33612345678")
                .build();
        
        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Updated"))
                .andExpect(jsonPath("$.data.lastName").value("Name"));
    }
    
    @Test
    void getProfile_Unauthorized_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void getSessions_Authenticated_ReturnsSessions() throws Exception {
        mockMvc.perform(get("/api/v1/users/sessions")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
    
    @Test
    void enable2FA_Authenticated_ReturnsSetupInfo() throws Exception {
        mockMvc.perform(post("/api/v1/users/2fa/enable")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.secret").exists())
                .andExpect(jsonPath("$.data.qrCodeUrl").exists())
                .andExpect(jsonPath("$.data.backupCodes").isArray());
    }
}
