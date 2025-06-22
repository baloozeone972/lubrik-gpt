package com.virtualcompanion.media.controller;

public class MediaProcessingService {

    private final ImageProcessor imageProcessor;
    private final VideoProcessor videoProcessor;
    private final AudioProcessor audioProcessor;
    private final RabbitTemplate rabbitTemplate;

    public Mono<MediaProcessingResponse> processMedia(UUID mediaId,
                                                      MediaProcessingRequest request,
                                                      String userId) {
        MediaProcessingJob job = MediaProcessingJob.builder()
                .jobId(UUID.randomUUID())
                .mediaId(mediaId)
                .userId(userId)
                .operations(request.getOperations())
                .priority(request.getPriority())
                .build();

        // Send to processing queue
        rabbitTemplate.convertAndSend("media.processing", job);

        return Mono.just(MediaProcessingResponse.builder()
                .jobId(job.getJobId())
                .status(ProcessingStatus.QUEUED)
                .estimatedTime(calculateEstimatedTime(request))
                .build());
    }

    private int calculateEstimatedTime(MediaProcessingRequest request) {
        return request.getOperations().stream()
                .mapToInt(op -> switch (op.getType()) {
                    case RESIZE -> 5;
                    case COMPRESS -> 10;
                    case TRANSCODE -> 30;
                    case WATERMARK -> 3;
                    case THUMBNAIL -> 2;
                    default -> 1;
                })
                .sum();
    }
}
