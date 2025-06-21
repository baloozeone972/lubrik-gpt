// MediaServiceApplication.java
package com.virtualcompanion.mediaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class MediaServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }
}

// application.yml
server:
  port: 8084
  servlet:
    context-path: /

spring:
  application:
    name: media-service
  
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:media_db}
    username: ${DB_USER:media_user}
    password: ${DB_PASSWORD:media_pass}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        show_sql: false
  
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: media-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
  
  servlet:
    multipart:
      enabled: true
      max-file-size: 100MB
      max-request-size: 100MB

# MinIO Configuration
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:minioadmin}
  secret-key: ${MINIO_SECRET_KEY:minioadmin}
  buckets:
    media: media-files
    thumbnails: media-thumbnails
    processed: media-processed
    temp: media-temp
  auto-create-bucket: true

# Kurento Media Server Configuration
kurento:
  url: ${KURENTO_URL:ws://localhost:8888/kurento}
  connection-timeout: 30000
  request-timeout: 60000
  heartbeat-interval: 30000

# FFmpeg Configuration
ffmpeg:
  path: ${FFMPEG_PATH:/usr/bin/ffmpeg}
  threads: ${FFMPEG_THREADS:4}
  timeout: ${FFMPEG_TIMEOUT:300000}
  
  # Video encoding presets
  presets:
    low:
      video-codec: libx264
      audio-codec: aac
      bitrate: 500k
      resolution: 640x360
    medium:
      video-codec: libx264
      audio-codec: aac
      bitrate: 1000k
      resolution: 1280x720
    high:
      video-codec: libx264
      audio-codec: aac
      bitrate: 2500k
      resolution: 1920x1080

# Voice Generation Configuration
voice:
  providers:
    elevenlabs:
      enabled: ${ELEVENLABS_ENABLED:false}
      api-key: ${ELEVENLABS_API_KEY:}
      api-url: https://api.elevenlabs.io/v1
      default-voice-id: ${ELEVENLABS_DEFAULT_VOICE:}
      max-chars-per-request: 5000
    
    azure:
      enabled: ${AZURE_TTS_ENABLED:true}
      api-key: ${AZURE_TTS_API_KEY:}
      region: ${AZURE_TTS_REGION:eastus}
      endpoint: https://${AZURE_TTS_REGION}.tts.speech.microsoft.com/
      default-voice: en-US-JennyNeural
    
    google:
      enabled: ${GOOGLE_TTS_ENABLED:false}
      credentials-path: ${GOOGLE_APPLICATION_CREDENTIALS:}
      default-language: en-US
      default-voice: en-US-Wavenet-D
  
  default-provider: azure
  cache-enabled: true
  cache-ttl: 86400 # 24 hours

# Media Processing Configuration
media:
  processing:
    max-concurrent-jobs: ${MAX_CONCURRENT_JOBS:10}
    job-timeout: ${JOB_TIMEOUT:300000}
    retry-attempts: 3
    retry-delay: 5000
  
  storage:
    max-file-size: ${MAX_FILE_SIZE:104857600} # 100MB
    allowed-video-formats:
      - video/mp4
      - video/webm
      - video/quicktime
    allowed-audio-formats:
      - audio/mpeg
      - audio/wav
      - audio/webm
      - audio/ogg
    allowed-image-formats:
      - image/jpeg
      - image/png
      - image/webp
      - image/gif
  
  streaming:
    chunk-size: 1048576 # 1MB
    buffer-size: 5242880 # 5MB
    
  thumbnails:
    width: 320
    height: 180
    quality: 0.8
    format: jpeg

# WebRTC Configuration
webrtc:
  stun-servers:
    - stun:stun.l.google.com:19302
    - stun:stun1.l.google.com:19302
  turn-servers:
    - url: ${TURN_URL:turn:localhost:3478}
      username: ${TURN_USERNAME:}
      credential: ${TURN_PASSWORD:}
  ice-candidate-timeout: 30000
  max-bandwidth:
    video: 2000 # kbps
    audio: 128 # kbps

# Security Configuration
security:
  jwt:
    public-key: ${JWT_PUBLIC_KEY}
  cors:
    allowed-origins:
      - http://localhost:3000
      - https://app.virtualcompanion.com
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
      - OPTIONS
    allowed-headers:
      - Authorization
      - Content-Type
      - X-Requested-With

# Rate Limiting
rate-limiting:
  enabled: true
  limits:
    - endpoint: /api/v1/media/upload
      requests-per-minute: 10
    - endpoint: /api/v1/media/*/transcode
      requests-per-minute: 5
    - endpoint: /api/v1/voice/generate
      requests-per-minute: 20

# Monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

# Logging
logging:
  level:
    root: INFO
    com.virtualcompanion.mediaservice: DEBUG
    org.kurento: DEBUG
    io.minio: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

# Database Migration Script Example
# V1__create_media_tables.sql
CREATE TABLE IF NOT EXISTS media_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    character_id UUID,
    conversation_id UUID,
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255),
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    duration DECIMAL(10,2),
    width INTEGER,
    height INTEGER,
    metadata JSONB,
    processing_status VARCHAR(50) DEFAULT 'pending',
    is_public BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS media_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    media_file_id UUID NOT NULL REFERENCES media_files(id),
    variant_type VARCHAR(50) NOT NULL,
    quality VARCHAR(20),
    format VARCHAR(20),
    storage_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    width INTEGER,
    height INTEGER,
    bitrate INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS streaming_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(100) UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    character_id UUID,
    conversation_id UUID,
    session_type VARCHAR(50) NOT NULL,
    kurento_session_id VARCHAR(200),
    sdp_offer TEXT,
    sdp_answer TEXT,
    ice_candidates JSONB,
    status VARCHAR(50) DEFAULT 'initializing',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,
    metadata JSONB
);

CREATE TABLE IF NOT EXISTS voice_generations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    character_id UUID NOT NULL,
    conversation_id UUID,
    text_content TEXT NOT NULL,
    text_hash VARCHAR(64),
    provider VARCHAR(50) NOT NULL,
    voice_id VARCHAR(100),
    voice_settings JSONB,
    output_format VARCHAR(20),
    storage_path VARCHAR(500),
    file_size BIGINT,
    duration DECIMAL(10,2),
    cost DECIMAL(10,4),
    status VARCHAR(50) DEFAULT 'pending',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_media_files_user_id ON media_files(user_id);
CREATE INDEX idx_media_files_character_id ON media_files(character_id);
CREATE INDEX idx_media_files_conversation_id ON media_files(conversation_id);
CREATE INDEX idx_media_files_processing_status ON media_files(processing_status);
CREATE INDEX idx_streaming_sessions_session_id ON streaming_sessions(session_id);
CREATE INDEX idx_streaming_sessions_user_id ON streaming_sessions(user_id);
CREATE INDEX idx_voice_generations_text_hash ON voice_generations(text_hash);
CREATE INDEX idx_voice_generations_character_id ON voice_generations(character_id);