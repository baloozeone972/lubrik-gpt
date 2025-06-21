// ModerationServiceApplication.java
package com.virtualcompanion.moderationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class ModerationServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ModerationServiceApplication.class, args);
    }
}

// application.yml
server:
  port: 8086
  servlet:
    context-path: /

spring:
  application:
    name: moderation-service
  
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:moderation_db}
    username: ${DB_USER:moderation_user}
    password: ${DB_PASSWORD:moderation_pass}
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
      group-id: moderation-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
      auto-offset-reset: earliest
  
  cache:
    type: redis
    redis:
      time-to-live: 3600000 # 1 hour
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

# AI Moderation Providers Configuration
ai:
  providers:
    openai:
      enabled: ${OPENAI_ENABLED:true}
      api-key: ${OPENAI_API_KEY}
      api-url: https://api.openai.com/v1
      model: gpt-4-vision-preview
      max-tokens: 150
      temperature: 0.2
    
    perspective:
      enabled: ${PERSPECTIVE_ENABLED:true}
      api-key: ${PERSPECTIVE_API_KEY}
      api-url: https://commentanalyzer.googleapis.com/v1alpha1
      threshold:
        toxicity: 0.7
        severe-toxicity: 0.5
        insult: 0.7
        threat: 0.7
        sexually-explicit: 0.8
    
    aws-rekognition:
      enabled: ${AWS_REKOGNITION_ENABLED:true}
      access-key: ${AWS_ACCESS_KEY}
      secret-key: ${AWS_SECRET_KEY}
      region: ${AWS_REGION:us-east-1}
      
    tensorflow:
      enabled: ${TENSORFLOW_ENABLED:false}
      model-path: ${TENSORFLOW_MODEL_PATH:/models/nsfw}
      threshold: 0.8

# Moderation Rules Configuration
moderation:
  rules:
    # Text moderation
    text:
      enabled: true
      providers: [openai, perspective]
      blacklist-words-file: classpath:moderation/blacklist.txt
      regex-patterns-file: classpath:moderation/patterns.json
      max-length: 5000
      languages: [en, es, fr, de, it, pt, ja, ko, zh]
    
    # Image moderation
    image:
      enabled: true
      providers: [aws-rekognition, tensorflow]
      max-size: 10485760 # 10MB
      allowed-formats: [image/jpeg, image/png, image/webp]
      categories:
        nudity:
          enabled: true
          threshold: 0.8
        violence:
          enabled: true
          threshold: 0.7
        hate-symbols:
          enabled: true
          threshold: 0.9
    
    # Voice moderation
    voice:
      enabled: true
      providers: [openai]
      max-duration: 300 # 5 minutes
      transcribe-first: true
    
    # Video moderation
    video:
      enabled: true
      providers: [aws-rekognition]
      max-duration: 600 # 10 minutes
      sample-interval: 5 # seconds
      
  # Age verification
  age-verification:
    enabled: true
    methods:
      self-declaration:
        enabled: true
        min-age: 13
      
      document-verification:
        enabled: true
        required-age: 18
        providers: [aws-rekognition]
        document-types: [passport, driver-license, id-card]
      
      payment-verification:
        enabled: true
        required-age: 18
        
  # Content filtering by age
  age-content-filters:
    under-13:
      block-all-user-content: true
      block-nsfw: true
      block-violence: true
      
    13-17:
      block-nsfw: true
      block-extreme-violence: true
      filter-profanity: true
      
    18-plus:
      block-illegal-content: true
      user-preference-based: true

# Jurisdiction-specific rules
jurisdictions:
  default:
    min-age: 13
    require-age-verification: true
    content-restrictions: standard
    
  EU:
    min-age: 16
    require-explicit-consent: true
    gdpr-compliant: true
    content-restrictions: strict
    
  US:
    min-age: 13
    coppa-compliant: true
    section-230-compliant: true
    state-specific-rules:
      CA:
        additional-privacy-rights: true
      TX:
        social-media-verification: true
        
  JP:
    min-age: 13
    content-restrictions: strict
    require-local-moderation: true

# Moderation Queue Configuration
queue:
  auto-moderation:
    enabled: true
    confidence-threshold: 0.85
    
  human-review:
    enabled: true
    escalation-threshold: 0.6
    priority-queues:
      - high: [child-safety, self-harm, terrorism]
      - medium: [harassment, hate-speech, violence]
      - low: [spam, mild-profanity]
    
  response-time-sla:
    high-priority: 3600 # 1 hour
    medium-priority: 14400 # 4 hours
    low-priority: 86400 # 24 hours

# Reporting Configuration
reporting:
  user-reports:
    enabled: true
    categories:
      - inappropriate-content
      - harassment
      - spam
      - underage-user
      - self-harm
      - other
    
  auto-reporting:
    enabled: true
    report-to-authorities:
      csam: true
      terrorism: true
      immediate-threat: true
    
  transparency-reports:
    enabled: true
    frequency: monthly
    include-metrics:
      - total-content-reviewed
      - violations-by-category
      - appeals-processed
      - average-response-time

# Security Configuration
security:
  jwt:
    public-key: ${JWT_PUBLIC_KEY}
    
  encryption:
    sensitive-content: true
    algorithm: AES-256

# Rate Limiting
rate-limiting:
  enabled: true
  limits:
    - endpoint: /api/v1/moderate/text
      requests-per-minute: 100
    - endpoint: /api/v1/moderate/image
      requests-per-minute: 50
    - endpoint: /api/v1/reports
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
    tags:
      application: ${spring.application.name}

# Logging
logging:
  level:
    root: INFO
    com.virtualcompanion.moderationservice: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"