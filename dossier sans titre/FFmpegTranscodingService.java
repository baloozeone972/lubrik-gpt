package com.virtualcompanion.mediaservice.service.impl;

public class FFmpegTranscodingService implements TranscodingService {
    
    private final MediaFileRepository mediaFileRepository;
    private final MediaVariantRepository variantRepository;
    private final StorageService storageService;
    
    @Value("${ffmpeg.path}")
    private String ffmpegPath;
    
    @Value("${ffmpeg.threads}")
    private int ffmpegThreads;
    
    @Value("${ffmpeg.timeout}")
    private long ffmpegTimeout;
    
    @Value("${ffmpeg.presets}")
    private Map<String, Map<String, String>> presets;
    
    @Override
    public UUID startTranscodingJob(MediaFile mediaFile, TranscodeRequest request) {
        UUID jobId = UUID.randomUUID();
        
        // Start async transcoding
        CompletableFuture.runAsync(() -> {
            try {
                processTranscoding(mediaFile, request, jobId);
            } catch (Exception e) {
                log.error("Transcoding failed for job {}: {}", jobId, e.getMessage());
                mediaFileRepository.updateProcessingStatus(mediaFile.getId(), "transcode_failed");
            }
        });
        
        return jobId;
    }
    
    @Override
    @Async
    public void startAsyncTranscoding(UUID mediaFileId, String preset) {
        try {
            MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                    .orElseThrow(() -> new TranscodingException("Media file not found"));
            
            TranscodeRequest request = TranscodeRequest.builder()
                    .qualityPreset(preset)
                    .generateThumbnails(true)
                    .thumbnailCount(5)
                    .build();
            
            processTranscoding(mediaFile, request, UUID.randomUUID());
            
        } catch (Exception e) {
            log.error("Async transcoding failed for media {}: {}", mediaFileId, e.getMessage());
        }
    }
    
    @Override
    public byte[] generateThumbnail(byte[] videoData) {
        try {
            // Save video data to temp file
            Path tempVideo = Files.createTempFile("video_", ".tmp");
            Files.write(tempVideo, videoData);
            
            // Generate thumbnail
            Path tempThumbnail = Files.createTempFile("thumb_", ".jpg");
            
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(tempVideo.toString());
            command.add("-ss");
            command.add("00:00:01"); // Take frame at 1 second
            command.add("-frames:v");
            command.add("1");
            command.add("-vf");
            command.add("scale=320:180");
            command.add("-y");
            command.add(tempThumbnail.toString());
            
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TranscodingException("Thumbnail generation timed out");
            }
            
            if (process.exitValue() != 0) {
                throw new TranscodingException("FFmpeg failed with exit code: " + process.exitValue());
            }
            
            byte[] thumbnailData = Files.readAllBytes(tempThumbnail);
            
            // Clean up temp files
            Files.deleteIfExists(tempVideo);
            Files.deleteIfExists(tempThumbnail);
            
            return thumbnailData;
            
        } catch (Exception e) {
            log.error("Failed to generate thumbnail: {}", e.getMessage());
            throw new TranscodingException("Failed to generate thumbnail: " + e.getMessage());
        }
    }
    
    private void processTranscoding(MediaFile mediaFile, TranscodeRequest request, UUID jobId) {
        log.info("Starting transcoding job {} for media {}", jobId, mediaFile.getId());
        
        try {
            // Update status
            mediaFileRepository.updateProcessingStatus(mediaFile.getId(), "transcoding");
            
            // Download original file
            byte[] originalData = storageService.getFileContent(mediaFile.getStoragePath());
            Path tempInput = Files.createTempFile("input_", getFileExtension(mediaFile.getFileName()));
            Files.write(tempInput, originalData);
            
            // Get preset settings
            Map<String, String> presetSettings = presets.get(request.getQualityPreset());
            if (presetSettings == null) {
                presetSettings = presets.get("medium"); // Default preset
            }
            
            // Transcode to requested format
            Path tempOutput = Files.createTempFile("output_", ".mp4");
            
            List<String> command = buildFFmpegCommand(
                    tempInput.toString(),
                    tempOutput.toString(),
                    presetSettings,
                    request
            );
            
            executeFFmpegCommand(command);
            
            // Upload transcoded file
            byte[] transcodedData = Files.readAllBytes(tempOutput);
            String variantPath = mediaFile.getStoragePath().replace(".", "_" + request.getQualityPreset() + ".");
            storageService.uploadBytes(transcodedData, variantPath);
            
            // Save variant record
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
            
            // Clean up temp files
            Files.deleteIfExists(tempInput);
            Files.deleteIfExists(tempOutput);
            
            // Update status
            mediaFileRepository.updateProcessingStatus(mediaFile.getId(), "completed");
            
            log.info("Transcoding completed for job {}", jobId);
            
        } catch (Exception e) {
            log.error("Transcoding failed for job {}: {}", jobId, e.getMessage());
            throw new TranscodingException("Transcoding failed: " + e.getMessage());
        }
    }
    
    private List<String> buildFFmpegCommand(String input, String output, 
                                           Map<String, String> preset, 
                                           TranscodeRequest request) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-i");
        command.add(input);
        command.add("-threads");
        command.add(String.valueOf(ffmpegThreads));
        
        // Video codec
        command.add("-c:v");
        command.add(preset.get("video-codec"));
        
        // Audio codec
        command.add("-c:a");
        command.add(preset.get("audio-codec"));
        
        // Bitrate
        command.add("-b:v");
        command.add(preset.get("bitrate"));
        
        // Resolution
        command.add("-vf");
        command.add("scale=" + preset.get("resolution"));
        
        // Additional options
        command.add("-preset");
        command.add("fast");
        command.add("-movflags");
        command.add("+faststart");
        
        command.add("-y"); // Overwrite output
        command.add(output);
        
        return command;
    }
    
    private void executeFFmpegCommand(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        // Log output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }
        
        boolean finished = process.waitFor(ffmpegTimeout, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new TranscodingException("FFmpeg process timed out");
        }
        
        if (process.exitValue() != 0) {
            throw new TranscodingException("FFmpeg failed with exit code: " + process.exitValue());
        }
    }
    
    private void generateVideoThumbnails(MediaFile mediaFile, Path videoPath, Integer count) {
        // Implementation would generate multiple thumbnails at different timestamps
        log.info("Generating {} thumbnails for media {}", count, mediaFile.getId());
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
    
    private Integer parseBitrate(String bitrate) {
        // Parse bitrate string like "1000k" to integer
        if (bitrate.endsWith("k")) {
            return Integer.parseInt(bitrate.substring(0, bitrate.length() - 1));
        }
        return Integer.parseInt(bitrate);
    }
}
