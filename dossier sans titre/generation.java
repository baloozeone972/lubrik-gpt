package com.virtualcompanion.mediaservice.service.impl;

record
                generation.setStoragePath(storagePath);
                generation.setFileSize((long) result.getAudioData().length);
                generation.setDuration(result.getDuration());
                generation.setCost(result.getCost());
                generation.setStatus("completed");
                generation.setCompletedAt(LocalDateTime.now());
                
                voiceGenerationRepository.save(generation);
                
                // Publish event
                publishVoiceGenerationEvent(generation, "completed");
                
                log.info("Voice generation completed: {}
