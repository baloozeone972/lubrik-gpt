package com.virtualcompanion.mediaservice.service;

record
            MediaVariant variant = MediaVariant.builder()
                    .mediaFileId(mediaFile.getId())
                    .variantType("transcode")
                    .quality(request.getQualityPreset())
                    .format("mp4")
                    .storagePath(variantPath)
                    .fileSize((long) transcodedData.length)
                    .width(Integer.parseInt(presetSettings.get("resolution").split("x")[0]))
                    .height(Integer.parseInt(presetSettings.get("resolution").split("x")[1]))
                    .bitrate(parseBitrate(presetSettings.get("bitrate")))
                    .build();
            
            variantRepository.save(variant);
            
            // Generate thumbnails if requested
            if (Boolean.TRUE.equals(request.getGenerateThumbnails())) {
                generateVideoThumbnails(mediaFile, tempInput, request.getThumbnailCount());
            }
