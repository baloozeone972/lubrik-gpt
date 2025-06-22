package com.virtualcompanion.userservice.service;

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
