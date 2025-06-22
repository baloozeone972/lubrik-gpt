package com.virtualcompanion.userservice.service.impl;

class UserServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserPreferenceRepository preferenceRepository;
    
    @Mock
    private VerificationTokenRepository tokenRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private FileStorageService fileStorageService;
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private RegisterRequest validRegisterRequest;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "ageVerificationEnabled", true);
        ReflectionTestUtils.setField(userService, "minimumAge", 18);
        
        validRegisterRequest = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("SecurePass123!")
                .firstName("Test")
                .lastName("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .acceptedTerms(true)
                .build();
        
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .password("encoded_password")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }
    
    @Test
    void register_Success() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        
        // When
        User result = userService.register(validRegisterRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(result.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
        
        verify(userRepository).save(any(User.class));
        verify(tokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendVerificationEmail(any(User.class), anyString());
        verify(kafkaTemplate).send(eq("user-events"), any(Map.class));
    }
    
    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> userService.register(validRegisterRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already registered");
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void register_UsernameAlreadyExists_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> userService.register(validRegisterRequest))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessage("Username already taken");
        
        verify(userRepository, never()).save(any());
    }
    
    @Test
    void changePassword_Success() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword")
                .newPassword("NewSecurePass123!")
                .confirmPassword("NewSecurePass123!")
                .build();
        
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewSecurePass123!")).thenReturn("new_encoded_password");
        
        // When
        userService.changePassword(testUser.getId(), request);
        
        // Then
        verify(userRepository).save(argThat(user -> 
            user.getPassword().equals("new_encoded_password")
        ));
        verify(emailService).sendPasswordChangedEmail(testUser);
    }
    
    @Test
    void changePassword_WrongCurrentPassword_ThrowsException() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongPassword")
                .newPassword("NewSecurePass123!")
                .confirmPassword("NewSecurePass123!")
                .build();
        
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);
        
        // When/Then
        assertThatThrownBy(() -> userService.changePassword(testUser.getId(), request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("Current password is incorrect");
    }
    
    @Test
    void verifyEmail_Success() {
        // Given
        String token = "valid-token";
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(testUser)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        
        testUser.setEmailVerified(false);
        testUser.setStatus(UserStatus.PENDING_VERIFICATION);
        
        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));
        
        // When
        userService.verifyEmail(token);
        
        // Then
        assertThat(testUser.isEmailVerified()).isTrue();
        assertThat(testUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(testUser);
        verify(emailService).sendWelcomeEmail(testUser);
    }
    
    @Test
    void verifyEmail_ExpiredToken_ThrowsException() {
        // Given
        String token = "expired-token";
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(testUser)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        
        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));
        
        // When/Then
        assertThatThrownBy(() -> userService.verifyEmail(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Verification token has expired");
    }
    
    @Test
    void uploadAvatar_Success() throws Exception {
        // Given
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/jpeg");
        
        String avatarUrl = "https://cdn.example.com/avatars/user-id.jpg";
        
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(fileStorageService.uploadFile(file, "avatars/" + testUser.getId())).thenReturn(avatarUrl);
        
        // When
        String result = userService.uploadAvatar(testUser.getId(), file);
        
        // Then
        assertThat(result).isEqualTo(avatarUrl);
        assertThat(testUser.getAvatarUrl()).isEqualTo(avatarUrl);
        verify(userRepository).save(testUser);
    }
    
    @Test
    void deleteAccount_ImmediateDelete_Success() {
        // Given
        AccountDeletionRequest request = AccountDeletionRequest.builder()
                .password("correctPassword")
                .reason("No longer needed")
                .immediateDelete(true)
                .build();
        
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getPassword(), testUser.getPassword())).thenReturn(true);
        
        // When
        userService.deleteAccount(testUser.getId(), request);
        
        // Then
        assertThat(testUser.getEmail()).startsWith("DELETED_");
        assertThat(testUser.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(kafkaTemplate).send(eq("billing-events"), any(Map.class));
        verify(emailService).sendAccountDeletionEmail(testUser);
    }
}
