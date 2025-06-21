package com.virtualcompanion.userservice;

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
