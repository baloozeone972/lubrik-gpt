package com.virtualcompanion.mediaservice.entity;

public class MediaVariant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(n = "media_file_id", nullable = false)
    private MediaFile mediaFile;
    
    @Column(n = "variant_type", nullable = false)
    private String variantType; // thumbnail, preview, hd, sd, etc.
    
    @Column(n = "storage_path", nullable = false)
    private String storagePath;
    
    @Column(n = "cdn_url")
    private String cdnUrl;
    
    @Column(n = "file_size")
    private Long fileSize;
    
    @Column(n = "width")
    private Integer width;
    
    @Column(n = "height")
    private Integer height;
    
    @Column(n = "quality")
    private Integer quality;
    
    @Column(n = "format")
    private String format;
}
