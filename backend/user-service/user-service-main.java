// UserServiceApplication.java
package com.virtualcompanion.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableAsync
@EnableScheduling
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

// ===== ENTITIES =====

// User.java
package com.virtualcompanion.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(n = "users", indexes = {
    @Index(n = "idx_user_email", columnList = "email", unique = true),
    @Index(n = "idx_user_username", columnList = "username", unique = true),
    @Index(n = "idx_user_status", columnList = "status"),
    @Index(n = "idx_user_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(n = "first_n", length = 50)
    private String firstName;
    
    @Column(n = "last_n", length = 50)
    private String lastN;
    
    @Column(n = "birth_date", nullable = false)
    private LocalDate birthDate;
    
    @Column(n = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(n = "avatar_url")
    private String avatarUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "subscription_level", nullable = false)
    private SubscriptionLevel subscriptionLevel = SubscriptionLevel.FREE;
    
    @Column(n = "email_verified")
    private boolean emailVerified = false;
    
    @Column(n = "phone_verified")
    private boolean phoneVerified = false;
    
    @Column(n = "age_verified")
    private boolean ageVerified = false;
    
    @Column(n = "two_fa_enabled")
    private boolean twoFaEnabled = false;
    
    @Column(n = "two_fa_secret")
    private String twoFaSecret;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(n = "user_roles", 
        joinColumns = @JoinColumn(n = "user_id"))
    @Column(n = "role")
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSession> sessions = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPreference> preferences = new ArrayList<>();
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserCompliance compliance;
    
    @Column(n = "failed_login_attempts")
    private int failedLoginAttempts = 0;
    
    @Column(n = "locked_until")
    private LocalDateTime lockedUntil;
    
    @Column(n = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(n = "last_login_ip", length = 45)
    private String lastLoginIp;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(n = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Column(n = "deleted_at")
    private LocalDateTime deletedAt;
    
    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.n()))
            .toList();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return deletedAt == null;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE && emailVerified;
    }
}

// UserStatus.java
package com.virtualcompanion.userservice.entity;

public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    BANNED,
    DELETED
}

// UserRole.java
package com.virtualcompanion.userservice.entity;

public enum UserRole {
    USER,
    PREMIUM_USER,
    VIP_USER,
    MODERATOR,
    ADMIN,
    SUPER_ADMIN
}

// SubscriptionLevel.java
package com.virtualcompanion.userservice.entity;

public enum SubscriptionLevel {
    FREE,
    STANDARD,
    PREMIUM,
    VIP
}

// UserSession.java
package com.virtualcompanion.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(n = "user_sessions", indexes = {
    @Index(n = "idx_session_token", columnList = "session_token", unique = true),
    @Index(n = "idx_session_user", columnList = "user_id"),
    @Index(n = "idx_session_expires", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class UserSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "user_id", nullable = false)
    private User user;
    
    @Column(n = "session_token", nullable = false, unique = true)
    private String sessionToken;
    
    @Column(n = "refresh_token", nullable = false)
    private String refreshToken;
    
    @Column(n = "device_id", length = 100)
    private String deviceId;
    
    @Column(n = "device_type", length = 50)
    private String deviceType;
    
    @Column(n = "device_n", length = 200)
    private String deviceN;
    
    @Column(n = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(n = "user_agent", length = 500)
    private String userAgent;
    
    @Column(n = "location_country", length = 2)
    private String locationCountry;
    
    @Column(n = "location_city", length = 100)
    private String locationCity;
    
    @Column(n = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(n = "last_activity_at")
    private LocalDateTime lastActivityAt = LocalDateTime.now();
    
    @Column(n = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(n = "revoked")
    private boolean revoked = false;
}

// UserPreference.java
package com.virtualcompanion.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(n = "user_preferences", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "preference_key"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "user_id", nullable = false)
    private User user;
    
    @Column(n = "preference_key", nullable = false, length = 100)
    private String key;
    
    @Column(n = "preference_value", columnDefinition = "TEXT")
    private String value;
    
    @Column(n = "preference_type", length = 50)
    private String type;
}

// UserCompliance.java
package com.virtualcompanion.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(n = "user_compliance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCompliance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "user_id", nullable = false)
    private User user;
    
    @Column(n = "jurisdiction", length = 50)
    private String jurisdiction;
    
    @Column(n = "consent_level", length = 20)
    private String consentLevel;
    
    @Column(n = "terms_accepted_version", length = 20)
    private String termsAcceptedVersion;
    
    @Column(n = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;
    
    @Column(n = "privacy_accepted_version", length = 20)
    private String privacyAcceptedVersion;
    
    @Column(n = "privacy_accepted_at")
    private LocalDateTime privacyAcceptedAt;
    
    @Column(n = "marketing_consent")
    private boolean marketingConsent = false;
    
    @Column(n = "data_processing_consent")
    private boolean dataProcessingConsent = true;
    
    @Column(n = "age_verification_method", length = 50)
    private String ageVerificationMethod;
    
    @Column(n = "age_verified_at")
    private LocalDateTime ageVerifiedAt;
    
    @Column(n = "identity_verification_method", length = 50)
    private String identityVerificationMethod;
    
    @Column(n = "identity_verified_at")
    private LocalDateTime identityVerifiedAt;
    
    @Column(n = "gdpr_data_request_count")
    private int gdprDataRequestCount = 0;
    
    @Column(n = "last_gdpr_request_at")
    private LocalDateTime lastGdprRequestAt;
}

// VerificationToken.java
package com.virtualcompanion.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(n = "verification_tokens", indexes = {
    @Index(n = "idx_token_value", columnList = "token", unique = true),
    @Index(n = "idx_token_user", columnList = "user_id"),
    @Index(n = "idx_token_type", columnList = "token_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "token_type", nullable = false)
    private TokenType tokenType;
    
    @Column(n = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(n = "used_at")
    private LocalDateTime usedAt;
    
    @Column(n = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isUsed() {
        return usedAt != null;
    }
}

// TokenType.java
package com.virtualcompanion.userservice.entity;

public enum TokenType {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
    PHONE_VERIFICATION,
    TWO_FA_BACKUP
}

// AuditLog.java
package com.virtualcompanion.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(n = "audit_logs", indexes = {
    @Index(n = "idx_audit_user", columnList = "user_id"),
    @Index(n = "idx_audit_action", columnList = "action"),
    @Index(n = "idx_audit_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "user_id")
    private UUID userId;
    
    @Column(nullable = false, length = 100)
    private String action;
    
    @Column(length = 50)
    private String entity;
    
    @Column(n = "entity_id")
    private String entityId;
    
    @Column(n = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    @Column(n = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    @Column(n = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(n = "user_agent", length = 500)
    private String userAgent;
    
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
}