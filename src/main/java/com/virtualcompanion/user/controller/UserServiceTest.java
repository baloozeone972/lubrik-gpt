package com.virtualcompanion.user.controller;

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
