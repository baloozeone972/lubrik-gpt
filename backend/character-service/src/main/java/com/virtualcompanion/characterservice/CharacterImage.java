package com.virtualcompanion.characterservice;

public class CharacterImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "character_id", nullable = false)
    private Character character;
    
    @Column(n = "image_url", nullable = false)
    private String imageUrl;
    
    @Column(n = "thumbnail_url")
    private String thumbnailUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "image_type", nullable = false)
    private ImageType imageType;
    
    @Column(n = "is_primary")
    private boolean isPrimary = false;
    
    @Column(n = "width")
    private Integer width;
    
    @Column(n = "height")
    private Integer height;
    
    @Column(n = "file_size")
    private Long fileSize;
    
    @Column(n = "mime_type")
    private String mimeType;
    
    @Column(n = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum ImageType {
        PROFILE,
        FULL_BODY,
        EXPRESSION,
        OUTFIT,
        SCENE,
        PROMOTIONAL
    }
}
