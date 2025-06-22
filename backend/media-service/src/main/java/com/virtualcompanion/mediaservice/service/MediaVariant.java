package com.virtualcompanion.mediaservice.service;

builder() =MediaVariant.

mediaFileId(mediaFile.getId()
                    .

variantType("transcode"))
        .

quality(request.getQualityPreset()
                    .

format("mp4"))
        .

storagePath(variantPath)
                    .

fileSize((long) transcodedData
                    .

width(Integer.parseInt(presetSettings.get("resolution").length)
        .

split("x")[.

height(Integer.parseInt(presetSettings.get("resolution")0]))
        .

split("x")[.

bitrate(parseBitrate(presetSettings.get("bitrate")1]))
        .

build();))
        .

save(variant);
            
            variantRepository.

equals(request.getGenerateThumbnails()

// Generate thumbnails if requested
            if(Boolean.TRUE.

generateVideoThumbnails(mediaFile, tempInput, request.getThumbnailCount())){

record
MediaVariant variant);
        }
