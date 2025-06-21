# Guide d'Intégration Frontend et Tests

## Table des Matières

1. [Architecture Frontend](#architecture-frontend)
2. [Configuration React/Next.js](#configuration-react)
3. [Intégration des Services](#integration-services)
4. [Tests Unitaires Backend](#tests-unitaires)
5. [Tests d'Intégration](#tests-integration)
6. [Tests End-to-End](#tests-e2e)
7. [Tests de Performance](#tests-performance)
8. [CI/CD Pipeline](#cicd-pipeline)

## Architecture Frontend {#architecture-frontend}

### Stack Technologique Recommandée

```yaml
Framework: Next.js 14 (App Router)
UI Library: React 18
State Management: Zustand + React Query
Styling: Tailwind CSS + Shadcn/ui
Real-time: Socket.io Client
Video: WebRTC + Simple-peer
Forms: React Hook Form + Zod
Testing: Jest + React Testing Library + Playwright
```

### Structure du Projet Frontend

```
frontend/
├── web-app/
│   ├── src/
│   │   ├── app/                    # Next.js App Router
│   │   │   ├── (auth)/            # Routes authentifiées
│   │   │   ├── (public)/          # Routes publiques
│   │   │   ├── api/              # Route handlers
│   │   │   └── layout.tsx        # Layout principal
│   │   ├── components/
│   │   │   ├── ui/               # Components réutilisables
│   │   │   ├── chat/             # Components de chat
│   │   │   ├── character/        # Components personnages
│   │   │   └── billing/          # Components facturation
│   │   ├── hooks/                # Custom React hooks
│   │   ├── lib/                  # Utilitaires
│   │   │   ├── api/             # Clients API
│   │   │   ├── websocket/       # WebSocket manager
│   │   │   └── webrtc/          # WebRTC helpers
│   │   ├── stores/              # Zustand stores
│   │   └── types/               # TypeScript types
│   ├── public/
│   └── tests/
└── mobile-app/                   # React Native
```

## Configuration React/Next.js {#configuration-react}

### Installation et Configuration de Base

```bash
# Créer le projet Next.js
npx create-next-app@latest web-app --typescript --tailwind --app

cd web-app

# Installer les dépendances essentielles
npm install @tanstack/react-query zustand socket.io-client
npm install simple-peer react-hook-form @hookform/resolvers zod
npm install @radix-ui/react-dialog @radix-ui/react-select
npm install react-markdown remark-gfm
npm install -D @types/simple-peer
```

### Configuration de l'Environnement

```typescript
// .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8083/ws
NEXT_PUBLIC_STRIPE_PUBLIC_KEY=pk_test_xxx
```

### Client API Principal

```typescript
// lib/api/client.ts
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      retry: 3,
    },
  },
});

class ApiClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  setToken(token: string | null) {
    this.token = token;
    if (token) {
      localStorage.setItem('auth_token', token);
    } else {
      localStorage.removeItem('auth_token');
    }
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;
    
    const config: RequestInit = {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(this.token && { Authorization: `Bearer ${this.token}` }),
        ...options.headers,
      },
    };

    const response = await fetch(url, config);

    if (!response.ok) {
      throw new ApiError(response.status, await response.text());
    }

    return response.json();
  }

  // Auth methods
  async login(email: string, password: string) {
    const response = await this.request<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    
    this.setToken(response.accessToken);
    return response;
  }

  async register(data: RegisterData) {
    return this.request<AuthResponse>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  // User methods
  async getProfile() {
    return this.request<UserProfile>('/users/profile');
  }

  // Character methods
  async getCharacters(params?: CharacterFilters) {
    const query = new URLSearchParams(params as any).toString();
    return this.request<CharacterList>(`/characters?${query}`);
  }

  // Conversation methods
  async createConversation(characterId: string) {
    return this.request<Conversation>('/conversations', {
      method: 'POST',
      body: JSON.stringify({ characterId }),
    });
  }
}

export const apiClient = new ApiClient(
  process.env.NEXT_PUBLIC_API_URL || ''
);
```

### WebSocket Manager

```typescript
// lib/websocket/manager.ts
import { io, Socket } from 'socket.io-client';

export class WebSocketManager {
  private socket: Socket | null = null;
  private listeners: Map<string, Set<Function>> = new Map();

  connect(url: string, token: string) {
    this.socket = io(url, {
      auth: { token },
      transports: ['websocket'],
      reconnection: true,
      reconnectionAttempts: 5,
      reconnectionDelay: 1000,
    });

    this.socket.on('connect', () => {
      console.log('WebSocket connected');
      this.emit('connected');
    });

    this.socket.on('disconnect', () => {
      console.log('WebSocket disconnected');
      this.emit('disconnected');
    });

    this.socket.on('message', (data) => {
      this.emit('message', data);
    });

    this.socket.on('typing', (data) => {
      this.emit('typing', data);
    });

    this.socket.on('error', (error) => {
      console.error('WebSocket error:', error);
      this.emit('error', error);
    });
  }

  disconnect() {
    this.socket?.disconnect();
    this.socket = null;
  }

  sendMessage(conversationId: string, content: string) {
    this.socket?.emit('message', {
      conversationId,
      content,
      timestamp: new Date().toISOString(),
    });
  }

  sendTyping(conversationId: string, isTyping: boolean) {
    this.socket?.emit('typing', {
      conversationId,
      isTyping,
    });
  }

  on(event: string, callback: Function) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)?.add(callback);
  }

  off(event: string, callback: Function) {
    this.listeners.get(event)?.delete(callback);
  }

  private emit(event: string, ...args: any[]) {
    this.listeners.get(event)?.forEach(callback => {
      callback(...args);
    });
  }
}

export const wsManager = new WebSocketManager();
```

### Store Zustand Principal

```typescript
// stores/useAppStore.ts
import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';

interface User {
  id: string;
  email: string;
  username: string;
  subscriptionLevel: string;
}

interface AppState {
  // Auth
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  
  // UI
  sidebarOpen: boolean;
  theme: 'light' | 'dark';
  
  // Actions
  setAuth: (user: User, token: string) => void;
  logout: () => void;
  toggleSidebar: () => void;
  setTheme: (theme: 'light' | 'dark') => void;
}

export const useAppStore = create<AppState>()(
  devtools(
    persist(
      (set) => ({
        // Initial state
        user: null,
        token: null,
        isAuthenticated: false,
        sidebarOpen: true,
        theme: 'light',
        
        // Actions
        setAuth: (user, token) =>
          set({
            user,
            token,
            isAuthenticated: true,
          }),
          
        logout: () =>
          set({
            user: null,
            token: null,
            isAuthenticated: false,
          }),
          
        toggleSidebar: () =>
          set((state) => ({ sidebarOpen: !state.sidebarOpen })),
          
        setTheme: (theme) => set({ theme }),
      }),
      {
        name: 'app-storage',
        partialize: (state) => ({
          token: state.token,
          theme: state.theme,
        }),
      }
    )
  )
);
```

## Intégration des Services {#integration-services}

### Hook pour les Conversations

```typescript
// hooks/useConversation.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/api/client';
import { wsManager } from '@/lib/websocket/manager';
import { useEffect, useState } from 'react';

export function useConversation(conversationId: string) {
  const queryClient = useQueryClient();
  const [messages, setMessages] = useState<Message[]>([]);
  const [isTyping, setIsTyping] = useState(false);

  // Charger les messages
  const { data: conversation } = useQuery({
    queryKey: ['conversation', conversationId],
    queryFn: () => apiClient.getConversation(conversationId),
  });

  // Envoyer un message
  const sendMessage = useMutation({
    mutationFn: (content: string) =>
      apiClient.sendMessage(conversationId, content),
    onSuccess: (newMessage) => {
      setMessages((prev) => [...prev, newMessage]);
    },
  });

  // WebSocket listeners
  useEffect(() => {
    const handleMessage = (message: Message) => {
      if (message.conversationId === conversationId) {
        setMessages((prev) => [...prev, message]);
      }
    };

    const handleTyping = (data: TypingEvent) => {
      if (data.conversationId === conversationId) {
        setIsTyping(data.isTyping);
      }
    };

    wsManager.on('message', handleMessage);
    wsManager.on('typing', handleTyping);

    return () => {
      wsManager.off('message', handleMessage);
      wsManager.off('typing', handleTyping);
    };
  }, [conversationId]);

  return {
    conversation,
    messages,
    isTyping,
    sendMessage: sendMessage.mutate,
    isLoading: sendMessage.isPending,
  };
}
```

### Component Chat Principal

```tsx
// components/chat/ChatWindow.tsx
import { useState, useRef, useEffect } from 'react';
import { useConversation } from '@/hooks/useConversation';
import { MessageList } from './MessageList';
import { MessageInput } from './MessageInput';
import { VideoChat } from './VideoChat';

interface ChatWindowProps {
  conversationId: string;
  characterId: string;
}

export function ChatWindow({ conversationId, characterId }: ChatWindowProps) {
  const { messages, isTyping, sendMessage } = useConversation(conversationId);
  const [showVideo, setShowVideo] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto-scroll
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b">
        <h2 className="text-xl font-semibold">Conversation</h2>
        <button
          onClick={() => setShowVideo(!showVideo)}
          className="p-2 hover:bg-gray-100 rounded-lg"
        >
          {showVideo ? 'Chat Text' : 'Video Chat'}
        </button>
      </div>

      {/* Content */}
      {showVideo ? (
        <VideoChat conversationId={conversationId} characterId={characterId} />
      ) : (
        <>
          <MessageList messages={messages} isTyping={isTyping} />
          <div ref={messagesEndRef} />
          <MessageInput onSend={sendMessage} />
        </>
      )}
    </div>
  );
}
```

## Tests Unitaires Backend {#tests-unitaires}

### Configuration des Tests

```xml
<!-- pom.xml additions -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <scope>test</scope>
</dependency>
```

### Test Service Utilisateur

```java
// UserServiceTest.java
package com.virtualcompanion.userservice.service;

import com.virtualcompanion.userservice.dto.request.RegisterRequest;
import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.exception.EmailAlreadyExistsException;
import com.virtualcompanion.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private RegisterRequest validRequest;
    
    @BeforeEach
    void setUp() {
        validRequest = RegisterRequest.builder()
            .email("test@example.com")
            .username("testuser")
            .password("SecurePass123!")
            .birthDate(LocalDate.of(1990, 1, 1))
            .acceptedTerms(true)
            .build();
    }
    
    @Test
    void registerUser_Success() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        
        // When
        User result = userService.register(validRequest);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(emailService).sendVerificationEmail(any(User.class));
    }
    
    @Test
    void registerUser_EmailExists_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> userService.register(validRequest))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessage("Email already registered");
    }
    
    @Test
    void changePassword_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
            .id(userId)
            .password("old_encoded_password")
            .build();
            
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "old_encoded_password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("new_encoded_password");
        
        // When
        userService.changePassword(userId, "oldPassword", "newPassword");
        
        // Then
        verify(userRepository).save(argThat(u -> 
            u.getPassword().equals("new_encoded_password")
        ));
    }
}
```

### Test d'Intégration Repository

```java
// UserRepositoryIntegrationTest.java
package com.virtualcompanion.userservice.repository;

import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class UserRepositoryIntegrationTest {
    
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
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void findByEmail_ExistingUser_ReturnsUser() {
        // Given
        User user = User.builder()
            .email("test@example.com")
            .username("testuser")
            .password("encoded_password")
            .birthDate(LocalDate.of(1990, 1, 1))
            .status(UserStatus.ACTIVE)
            .build();
        entityManager.persistAndFlush(user);
        
        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }
    
    @Test
    void countByStatusAndCreatedAtBetween_ReturnsCorrectCount() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        createUser("user1@example.com", UserStatus.ACTIVE);
        createUser("user2@example.com", UserStatus.ACTIVE);
        createUser("user3@example.com", UserStatus.PENDING_VERIFICATION);
        entityManager.flush();
        
        // When
        long count = userRepository.countByStatusAndCreatedAtBetween(
            UserStatus.ACTIVE, startDate, endDate
        );
        
        // Then
        assertThat(count).isEqualTo(2);
    }
    
    private User createUser(String email, UserStatus status) {
        User user = User.builder()
            .email(email)
            .username(email.split("@")[0])
            .password("password")
            .birthDate(LocalDate.of(1990, 1, 1))
            .status(status)
            .build();
        return entityManager.persist(user);
    }
}
```

## Tests d'Intégration {#tests-integration}

### Test API Controller

```java
// UserControllerIntegrationTest.java
package com.virtualcompanion.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualcompanion.userservice.dto.request.UpdateProfileRequest;
import com.virtualcompanion.userservice.entity.User;
import com.virtualcompanion.userservice.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    private String authToken;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        authToken = jwtTokenProvider.generateAccessToken(testUser);
    }
    
    @Test
    void getProfile_Authenticated_ReturnsProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.data.username").value(testUser.getUsername()));
    }
    
    @Test
    void updateProfile_ValidData_UpdatesSuccessfully() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+33612345678")
                .build();
        
        mockMvc.perform(put("/api/v1/users/profile")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"));
    }
    
    @Test
    void getProfile_Unauthorized_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized());
    }
}
```

## Tests End-to-End {#tests-e2e}

### Configuration Playwright

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
  ],

  webServer: {
    command: 'npm run dev',
    port: 3000,
    reuseExistingServer: !process.env.CI,
  },
});
```

### Test E2E - Parcours Utilisateur

```typescript
// tests/e2e/user-journey.spec.ts
import { test, expect } from '@playwright/test';

test.describe('User Journey', () => {
  test('Complete user flow: register, login, chat', async ({ page }) => {
    // 1. Registration
    await page.goto('/register');
    
    await page.fill('[name="email"]', 'test@example.com');
    await page.fill('[name="username"]', 'testuser');
    await page.fill('[name="password"]', 'SecurePass123!');
    await page.fill('[name="birthDate"]', '1990-01-01');
    await page.check('[name="acceptedTerms"]');
    
    await page.click('button[type="submit"]');
    
    // Wait for redirect to dashboard
    await expect(page).toHaveURL('/dashboard');
    
    // 2. Browse Characters
    await page.click('text=Explorer les personnages');
    await expect(page).toHaveURL('/characters');
    
    // Filter by category
    await page.selectOption('[name="category"]', 'FRIEND');
    await page.waitForLoadState('networkidle');
    
    // Select a character
    await page.click('.character-card:first-child');
    await expect(page).toHaveURL(/\/characters\/[a-z0-9-]+/);
    
    // 3. Start Conversation
    await page.click('text=Commencer une conversation');
    await page.waitForURL(/\/chat\/[a-z0-9-]+/);
    
    // Send a message
    const messageInput = page.locator('[name="message"]');
    await messageInput.fill('Bonjour, comment vas-tu ?');
    await messageInput.press('Enter');
    
    // Wait for response
    await expect(page.locator('.message.character')).toBeVisible();
    
    // 4. Test Video Chat
    await page.click('text=Video Chat');
    
    // Grant permissions (mock in test environment)
    await page.context().grantPermissions(['camera', 'microphone']);
    
    // Wait for video stream
    await expect(page.locator('video#localVideo')).toBeVisible();
    await expect(page.locator('video#remoteVideo')).toBeVisible();
    
    // 5. Check Subscription
    await page.goto('/subscription');
    await expect(page.locator('text=Plan Gratuit')).toBeVisible();
    
    // Try to upgrade
    await page.click('text=Passer au Standard');
    await expect(page).toHaveURL('/subscription/upgrade');
  });
});
```

## Tests de Performance {#tests-performance}

### JMeter Test Plan

```xml
<!-- user-service-load-test.jmx -->
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0">
  <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="User Service Load Test">
    <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
      <collectionProp name="Arguments.arguments">
        <elementProp name="BASE_URL" elementType="Argument">
          <stringProp name="Argument.name">BASE_URL</stringProp>
          <stringProp name="Argument.value">http://localhost:8080</stringProp>
        </elementProp>
      </collectionProp>
    </elementProp>
  </TestPlan>
  
  <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="API Load Test">
    <stringProp name="ThreadGroup.num_threads">100</stringProp>
    <stringProp name="ThreadGroup.ramp_time">30</stringProp>
    <stringProp name="ThreadGroup.duration">300</stringProp>
    
    <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Login Request">
      <stringProp name="HTTPSampler.path">/api/v1/auth/login</stringProp>
      <stringProp name="HTTPSampler.method">POST</stringProp>
      <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
      <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="email" elementType="HTTPArgument">
            <stringProp name="Argument.value">test${__threadNum}@example.com</stringProp>
          </elementProp>
          <elementProp name="password" elementType="HTTPArgument">
            <stringProp name="Argument.value">Password123!</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </HTTPSamplerProxy>
    
    <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Response Code Assertion">
      <collectionProp name="Asserion.test_strings">
        <stringProp name="49586">200</stringProp>
      </collectionProp>
      <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
    </ResponseAssertion>
  </ThreadGroup>
</jmeterTestPlan>
```

### Gatling Performance Test

```scala
// ConversationLoadTest.scala
package com.virtualcompanion.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class ConversationLoadTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
  
  val authToken = "test_token_here"
  
  val conversationScenario = scenario("Conversation Load Test")
    // Login
    .exec(http("Login")
      .post("/api/v1/auth/login")
      .body(StringBody("""{"email":"test@example.com","password":"Password123!"}"""))
      .check(jsonPath("$.accessToken").saveAs("token"))
    )
    
    // Create conversation
    .exec(http("Create Conversation")
      .post("/api/v1/conversations")
      .header("Authorization", "Bearer ${token}")
      .body(StringBody("""{"characterId":"character-123"}"""))
      .check(jsonPath("$.id").saveAs("conversationId"))
    )
    
    // Send messages
    .repeat(10) {
      exec(http("Send Message")
        .post("/api/v1/conversations/${conversationId}/messages")
        .header("Authorization", "Bearer ${token}")
        .body(StringBody("""{"content":"Test message ${counter}"}"""))
      )
      .pause(1, 3)
    }
  
  setUp(
    conversationScenario.inject(
      rampUsers(100) during (2 minutes),
      constantUsersPerSec(20) during (5 minutes),
      rampUsers(50) during (1 minute)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.max.lt(3000),
     global.successfulRequests.percent.gt(95)
   )
}
```

## CI/CD Pipeline {#cicd-pipeline}

### GitHub Actions Workflow

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  JAVA_VERSION: '21'
  NODE_VERSION: '18'

jobs:
  # Backend Tests
  backend-test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test
          POSTGRES_DB: test_db
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379
    
    strategy:
      matrix:
        service: [user-service, character-service, conversation-service, media-service, billing-service, moderation-service]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
      
      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
      
      - name: Run tests
        working-directory: backend/${{ matrix.service }}
        run: |
          mvn clean test
          mvn jacoco:report
      
      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          file: backend/${{ matrix.service }}/target/site/jacoco/jacoco.xml
          flags: ${{ matrix.service }}
  
  # Frontend Tests
  frontend-test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/web-app/package-lock.json
      
      - name: Install dependencies
        working-directory: frontend/web-app
        run: npm ci
      
      - name: Run linting
        working-directory: frontend/web-app
        run: npm run lint
      
      - name: Run unit tests
        working-directory: frontend/web-app
        run: npm run test:ci
      
      - name: Build
        working-directory: frontend/web-app
        run: npm run build
  
  # E2E Tests
  e2e-test:
    runs-on: ubuntu-latest
    needs: [backend-test, frontend-test]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: ${{ env.NODE_VERSION }}
      
      - name: Start services with Docker Compose
        run: |
          docker-compose -f docker-compose.test.yml up -d
          ./scripts/wait-for-services.sh
      
      - name: Install Playwright
        working-directory: frontend/web-app
        run: |
          npm ci
          npx playwright install --with-deps
      
      - name: Run E2E tests
        working-directory: frontend/web-app
        run: npm run test:e2e
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: frontend/web-app/playwright-report
  
  # Security Scan
  security-scan:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'sarif'
          output: 'trivy-results.sarif'
      
      - name: Upload Trivy scan results
        uses: github/codeql-action/upload-sarif@v2
        with:
          sarif_file: 'trivy-results.sarif'
  
  # Build and Push Docker Images
  build-push:
    runs-on: ubuntu-latest
    needs: [backend-test, frontend-test, e2e-test]
    if: github.ref == 'refs/heads/main'
    
    strategy:
      matrix:
        service: [user-service, character-service, conversation-service, media-service, billing-service, moderation-service, gateway, web-app]
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2
      
      - name: Login to DockerHub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      
      - name: Build and push
        uses: docker/build-push-action@v4
        with:
          context: ${{ matrix.service == 'web-app' && 'frontend/web-app' || format('backend/{0}', matrix.service) }}
          push: true
          tags: |
            virtualcompanion/${{ matrix.service }}:latest
            virtualcompanion/${{ matrix.service }}:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
  
  # Deploy to Kubernetes
  deploy:
    runs-on: ubuntu-latest
    needs: build-push
    if: github.ref == 'refs/heads/main'
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup kubectl
        uses: azure/setup-kubectl@v3
      
      - name: Configure kubectl
        run: |
          echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > kubeconfig
          export KUBECONFIG=kubeconfig
      
      - name: Update deployments
        run: |
          kubectl set image deployment/user-service user-service=virtualcompanion/user-service:${{ github.sha }}
          kubectl set image deployment/character-service character-service=virtualcompanion/character-service:${{ github.sha }}
          # ... autres services
          
      - name: Wait for rollout
        run: |
          kubectl rollout status deployment/user-service
          kubectl rollout status deployment/character-service
          # ... autres services
```

### Script de Tests Locaux

```bash
#!/bin/bash
# run-all-tests.sh

echo "🧪 Running Virtual Companion Test Suite"

# Backend unit tests
echo "📦 Running backend unit tests..."
for service in user-service character-service conversation-service media-service billing-service moderation-service; do
    echo "Testing $service..."
    cd backend/$service
    mvn clean test || exit 1
    cd ../..
done

# Frontend tests
echo "🎨 Running frontend tests..."
cd frontend/web-app
npm test || exit 1
cd ../..

# Integration tests
echo "🔗 Running integration tests..."
docker-compose -f docker-compose.test.yml up -d
./scripts/wait-for-services.sh

cd backend
mvn verify -P integration-tests || exit 1
cd ..

# E2E tests
echo "🌐 Running E2E tests..."
cd frontend/web-app
npm run test:e2e || exit 1
cd ../..

# Performance tests
echo "⚡ Running performance tests..."
cd tests/performance
mvn gatling:test || exit 1
cd ../..

# Cleanup
docker-compose -f docker-compose.test.yml down

echo "✅ All tests passed!"
```

## Conclusion

Cette documentation complète couvre l'ensemble du développement, du déploiement et des tests de l'application Virtual Companion. Les points clés à retenir :

1. **Architecture modulaire** : Chaque service est indépendant et peut être développé/déployé séparément
2. **Tests complets** : Unitaires, intégration, E2E et performance
3. **CI/CD automatisé** : Déploiement continu avec validation à chaque étape
4. **Monitoring intégré** : Métriques et logs pour tous les services
5. **Sécurité renforcée** : Authentification, modération et conformité légale

Pour démarrer le développement :
1. Cloner le repository
2. Configurer l'environnement (.env)
3. Lancer l'infrastructure (docker-compose)
4. Développer avec hot-reload
5. Tester continuellement
6. Déployer via CI/CD

L'application est maintenant prête pour un développement en équipe et un déploiement en production!