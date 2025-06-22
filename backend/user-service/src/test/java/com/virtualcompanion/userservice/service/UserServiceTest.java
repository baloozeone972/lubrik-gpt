// UserServiceTest.java
package com.virtualcompanion.userservice.service;

import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.entity.UserCompliance;
import com.virtualcompanion.userservice.entity.UserPreference;
import com.virtualcompanion.userservice.entity.VerificationToken;
import com.virtualcompanion.userservice.exception.ResourceNotFoundException;
import com.virtualcompanion.userservice.exception.UserAlreadyExistsException;
import com.virtualcompanion.userservice.mapper.UserMapper;
import com.virtualcompanion.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
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
    private UserSessionRepository sessionRepository;

    @Mock
    private UserPreferenceRepository preferenceRepository;

    @Mock
    private UserComplianceRepository complianceRepository;

    @Mock
    private VerificationTokenRepository tokenRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private TwoFactorService twoFactorService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private RegisterRequest registerRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .passwordHash("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .age(25)
                .phoneNumber("+1234567890")
                .locale("en_US")
                .subscriptionLevel(User.SubscriptionLevel.FREE)
                .isActive(true)
                .emailVerified(false)
                .twoFactorEnabled(false)
                .build();

        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .age(25)
                .phoneNumber("+1234567890")
                .locale("en_US")
                .jurisdiction("US")
                .build();

        userResponse = UserResponse.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .build();
    }

    @Test
    void createUser_Success() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // When
        UserResponse result = userService.createUser(registerRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(registerRequest.getEmail());

        verify(userRepository).save(any(User.class));
        verify(preferenceRepository).save(any(UserPreference.class));
        verify(complianceRepository).save(any(UserCompliance.class));
        verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
        verify(kafkaTemplate).send(eq("user-events"), eq("user.created"), any());
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("User with email already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_UsernameAlreadyExists_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_Success() {
        // Given
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(userResponse);

        // When
        UserResponse result = userService.getUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateUser_Success() {
        // Given
        UUID userId = testUser.getId();
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .firstName("Updated")
                .lastName("Name")
                .phoneNumber("+9876543210")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        // When
        UserResponse result = userService.updateUser(userId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(kafkaTemplate).send(eq("user-events"), eq("user.updated"), any());
    }

    @Test
    void deleteUser_Success() {
        // Given
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.deleteUser(userId);

        // Then
        verify(userRepository).save(argThat(user ->
                !user.getIsActive() && user.getDeletedAt() != null
        ));
        verify(sessionRepository).invalidateAllUserSessions(userId);
        verify(kafkaTemplate).send(eq("user-events"), eq("user.deleted"), any());
    }

    @Test
    void verifyEmail_Success() {
        // Given
        String token = "verificationToken";
        VerificationToken verificationToken = VerificationToken.builder()
                .userId(testUser.getId())
                .token(token)
                .type(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(verificationToken));

        // When
        userService.verifyEmail(token);

        // Then
        verify(userRepository).verifyEmail(testUser.getId());
        verify(tokenRepository).markAsUsed(token, any(LocalDateTime.class));
        verify(kafkaTemplate).send(eq("user-events"), eq("user.email.verified"), any());
    }

    @Test
    void updatePassword_Success() {
        // Given
        UUID userId = testUser.getId();
        UpdatePasswordRequest request = UpdatePasswordRequest.builder()
                .currentPassword("oldPassword")
                .newPassword("newPassword")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", testUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        // When
        UserResponse result = userService.updatePassword(userId, request);

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
        verify(sessionRepository).invalidateAllUserSessions(userId);
        verify(kafkaTemplate).send(eq("user-events"), eq("user.password.changed"), any());
    }

    @Test
    void enableTwoFactorAuth_Success() {
        // Given
        UUID userId = testUser.getId();
        testUser.setTwoFactorEnabled(false);
        String secret = "ABCDEF123456";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(twoFactorService.generateSecret()).thenReturn(secret);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.enableTwoFactorAuth(userId);

        // Then
        verify(userRepository).save(argThat(user ->
                user.isTwoFactorEnabled() && secret.equals(user.getTwoFactorSecret())
        ));
        verify(kafkaTemplate).send(eq("user-events"), eq("user.2fa.enabled"), any());
    }
}
