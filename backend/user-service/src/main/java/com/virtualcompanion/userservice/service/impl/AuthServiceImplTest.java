package com.virtualcompanion.userservice.service.impl;

class AuthServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private UserSessionRepository sessionRepository;
    
    @Mock
    private VerificationTokenRepository tokenRepository;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private UserService userService;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private SessionService sessionService;
    
    @Mock
    private TwoFactorService twoFactorService;
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Mock
    private HttpServletRequest httpServletRequest;
    
    @InjectMocks
    private AuthServiceImpl authService;
    
    private User testUser;
    private LoginRequest loginRequest;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .password("encoded_password")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .twoFaEnabled(false)
                .build();
        
        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();
    }
    
    @Test
    void login_Success() {
        // Given
        Authentication authentication = mock(Authentication.class);
        UserSession session = UserSession.builder()
                .sessionToken("session-token")
                .build();
        
        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(sessionService.createSession(eq(testUser), eq(loginRequest), eq(httpServletRequest)))
                .thenReturn(session);
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(testUser, session.getSessionToken()))
                .thenReturn("refresh-token");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(3600000L);
        
        // When
        AuthResponse response = authService.login(loginRequest, httpServletRequest);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRequiresTwoFactor()).isFalse();
        
        verify(userRepository).save(argThat(user -> 
            user.getFailedLoginAttempts() == 0 && user.getLockedUntil() == null
        ));
    }
    
    @Test
    void login_With2FA_ReturnsRequires2FA() {
        // Given
        testUser.setTwoFaEnabled(true);
        Authentication authentication = mock(Authentication.class);
        
        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        
        // When
        AuthResponse response = authService.login(loginRequest, httpServletRequest);
        
        // Then
        assertThat(response.getRequiresTwoFactor()).isTrue();
        assertThat(response.getAccessToken()).isNull();
    }
    
    @Test
    void login_AccountLocked_ThrowsException() {
        // Given
        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        
        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase()))
                .thenReturn(Optional.of(testUser));
        
        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest, httpServletRequest))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Account is locked until");
    }
    
    @Test
    void login_InvalidCredentials_IncrementsFailedAttempts() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail().toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        
        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest, httpServletRequest))
                .isInstanceOf(BadCredentialsException.class);
        
        verify(userRepository).save(argThat(user -> 
            user.getFailedLoginAttempts() == 1
        ));
    }
    
    @Test
    void refreshToken_Success() {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        String sessionToken = "session-token";
        UserSession session = UserSession.builder()
                .sessionToken(sessionToken)
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .revoked(false)
                .build();
        
        when(jwtTokenProvider.validateToken(request.getRefreshToken())).thenReturn(true);
        when(jwtTokenProvider.getSessionTokenFromJwt(request.getRefreshToken())).thenReturn(sessionToken);
        when(sessionRepository.findBySessionToken(sessionToken)).thenReturn(Optional.of(session));
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(3600000L);
        
        // When
        AuthResponse response = authService.refreshToken(request);
        
        // Then
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(request.getRefreshToken());
        verify(sessionRepository).save(session);
    }
}
