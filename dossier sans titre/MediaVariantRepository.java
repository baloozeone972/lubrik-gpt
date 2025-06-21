package com.virtualcompanion.mediaservice.repository;

public interface MediaVariantRepository extends JpaRepository<MediaVariant, UUID> {
    
    List<MediaVariant> findByMediaFileId(UUID mediaFileId);
    
    Optional<MediaVariant> findByMediaFileIdAndVariantTypeAndQuality(UUID mediaFileId, String variantType, String quality);
    
    @Query("SELECT SUM(v.fileSize) FROM MediaVariant v WHERE v.mediaFileId = :mediaFileId")
    Long getTotalVariantsSizeByMediaFile(@Param("mediaFileId") UUID mediaFileId);
    
    void deleteByMediaFileId(UUID mediaFileId);
}
