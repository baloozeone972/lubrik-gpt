// BillingServiceApplication.java
package com.virtualcompanion.billingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableAsync
public class BillingServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}

// application.yml
server:
  port: 8085
  servlet:
    context-path: /

spring:
  application:
    name: billing-service
  
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:billing_db}
    username: ${DB_USER:billing_user}
    password: ${DB_PASSWORD:billing_pass}
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
      group-id: billing-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
  
  cache:
    type: redis
    redis:
      time-to-live: 3600000 # 1 hour
  
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

# Quartz Scheduler Configuration
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: always
    properties:
      org:
        quartz:
          scheduler:
            instanceId: AUTO
            instanceName: BillingScheduler
          jobStore:
            driverDelegateClass: org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
            useProperties: false
            tablePrefix: QRTZ_
            isClustered: true
            clusterCheckinInterval: 5000
          threadPool:
            threadCount: 10

# Stripe Configuration
stripe:
  api-key: ${STRIPE_API_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}
  api-version: "2023-10-16"
  
  # Product IDs
  products:
    free:
      id: ${STRIPE_FREE_PRODUCT_ID:prod_free}
      price-id: ${STRIPE_FREE_PRICE_ID:price_free}
    standard:
      id: ${STRIPE_STANDARD_PRODUCT_ID:prod_standard}
      price-id: ${STRIPE_STANDARD_PRICE_ID:price_standard}
    premium:
      id: ${STRIPE_PREMIUM_PRODUCT_ID:prod_premium}
      price-id: ${STRIPE_PREMIUM_PRICE_ID:price_premium}
    vip:
      id: ${STRIPE_VIP_PRODUCT_ID:prod_vip}
      price-id: ${STRIPE_VIP_PRICE_ID:price_vip}

# PayPal Configuration
paypal:
  client-id: ${PAYPAL_CLIENT_ID}
  client-secret: ${PAYPAL_CLIENT_SECRET}
  mode: ${PAYPAL_MODE:sandbox} # sandbox or live
  webhook-id: ${PAYPAL_WEBHOOK_ID}
  
  # Plan IDs
  plans:
    standard: ${PAYPAL_STANDARD_PLAN_ID}
    premium: ${PAYPAL_PREMIUM_PLAN_ID}
    vip: ${PAYPAL_VIP_PLAN_ID}

# Subscription Plans Configuration
subscription:
  plans:
    free:
      name: "Free"
      price: 0.00
      currency: EUR
      features:
        max-characters: 3
        max-conversations-per-day: 10
        voice-generation: false
        video-chat: false
        custom-characters: false
    
    standard:
      name: "Standard"
      price: 9.99
      currency: EUR
      trial-days: 7
      features:
        max-characters: 10
        max-conversations-per-day: -1
        voice-generation: true
        video-chat: false
        custom-characters: true
        voice-minutes-per-month: 100
    
    premium:
      name: "Premium"
      price: 19.99
      currency: EUR
      trial-days: 14
      features:
        max-characters: 50
        max-conversations-per-day: -1
        voice-generation: true
        video-chat: true
        custom-characters: true
        voice-minutes-per-month: 500
        video-minutes-per-month: 120
    
    vip:
      name: "VIP"
      price: 39.99
      currency: EUR
      trial-days: 14
      features:
        max-characters: -1
        max-conversations-per-day: -1
        voice-generation: true
        video-chat: true
        custom-characters: true
        voice-minutes-per-month: -1
        video-minutes-per-month: -1
        priority-support: true
        early-access: true

# Usage Tracking Configuration
usage:
  tracking:
    conversation-cost: 0.001 # per message
    voice-cost: 0.01 # per minute
    video-cost: 0.05 # per minute
    storage-cost: 0.0001 # per MB per month
  
  limits:
    enforcement: strict # strict or soft
    grace-period: 3 # days
    overage-allowed: false

# Invoice Configuration
invoice:
  company:
    name: "Virtual Companion Inc."
    address: "123 Tech Street"
    city: "San Francisco"
    state: "CA"
    zip: "94105"
    country: "USA"
    tax-id: "XX-XXXXXXX"
  
  template-path: classpath:templates/invoice.html
  storage-path: /invoices
  
  tax:
    default-rate: 0.0
    rates:
      US: 0.0
      EU: 0.20
      UK: 0.20
      CA: 0.05

# Payment Retry Configuration
payment:
  retry:
    max-attempts: 3
    intervals: [1, 3, 7] # days
    
  grace-period: 7 # days before suspension
  
  methods:
    card:
      enabled: true
      providers: [stripe, paypal]
    bank:
      enabled: false
      providers: []
    crypto:
      enabled: false
      providers: []

# Security Configuration
security:
  jwt:
    public-key: ${JWT_PUBLIC_KEY}
  
  pci:
    tokenization: true
    encrypt-sensitive-data: true
    audit-logging: true

# Webhook Configuration
webhooks:
  retry-attempts: 3
  timeout: 30000
  
  endpoints:
    stripe: /api/v1/webhooks/stripe
    paypal: /api/v1/webhooks/paypal

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
    com.virtualcompanion.billingservice: DEBUG
    com.stripe: INFO
    org.springframework.web: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

# Rate Limiting
rate-limiting:
  enabled: true
  limits:
    - endpoint: /api/v1/subscriptions/*/cancel
      requests-per-minute: 5
    - endpoint: /api/v1/payments/refund
      requests-per-minute: 10