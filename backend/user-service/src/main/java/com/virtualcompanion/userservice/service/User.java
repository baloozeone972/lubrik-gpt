package com.virtualcompanion.userservice.service;

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
