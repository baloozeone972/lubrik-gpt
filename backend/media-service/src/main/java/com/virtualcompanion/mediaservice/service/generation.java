package com.virtualcompanion.mediaservice.service;

setStoragePath(storagePath);.

setFileSize((long) result
                generation.

getAudioData().

setDuration(result.getDuration().length);
        generation.

setCost(result.getCost());
        generation.

setStatus("completed"););
        generation.

setCompletedAt(LocalDateTime.now()
                generation.

save(generation););

        voiceGenerationRepository.

// Publish event
publishVoiceGenerationEvent(generation, "completed");

info("Voice generation completed: {}
                
                log.

record
generation
