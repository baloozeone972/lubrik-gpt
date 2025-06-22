package com.virtualcompanion.media.controller;

public class GatewayConfiguration {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
                                           AuthenticationGatewayFilter authFilter,
                                           RateLimitingGatewayFilter rateLimitFilter,
                                           LoggingGatewayFilter loggingFilter) {
        return builder.routes()
                // User Service
                .route("user-service", r -> r
                        .path("/api/v1/users/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(200, 60))))
                        .uri("lb://USER-SERVICE"))

                // Character Service
                .route("character-service", r -> r
                        .path("/api/v1/characters/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(100, 60))))
                        .uri("lb://CHARACTER-SERVICE"))

                // Conversation Service
                .route("conversation-service", r -> r
                        .path("/api/v1/conversations/**", "/api/v1/messages/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(300, 60))))
                        .uri("lb://CONVERSATION-SERVICE"))

                // Media Service
                .route("media-service", r -> r
                        .path("/api/v1/media/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(50, 60))))
                        .uri("lb://MEDIA-SERVICE"))

                // Moderation Service
                .route("moderation-service", r -> r
                        .path("/api/v1/moderation/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(100, 60))))
                        .uri("lb://MODERATION-SERVICE"))

                // Billing Service
                .route("billing-service", r -> r
                        .path("/api/v1/billing/**", "/api/v1/subscriptions/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(authFilter.apply(new AuthenticationGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(50, 60))))
                        .uri("lb://BILLING-SERVICE"))

                // WebSocket Route
                .route("websocket", r -> r
                        .path("/ws/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config())))
                        .uri("lb:ws://CONVERSATION-SERVICE"))

                // Public routes (no auth)
                .route("public", r -> r
                        .path("/api/v1/auth/**", "/api/v1/public/**")
                        .filters(f -> f
                                .filter(loggingFilter.apply(new LoggingGatewayFilter.Config()))
                                .filter(rateLimitFilter.apply(createRateLimitConfig(30, 60))))
                        .uri("lb://USER-SERVICE"))

                .build();
    }

    private RateLimitingGatewayFilter.Config createRateLimitConfig(int capacity, int period) {
        RateLimitingGatewayFilter.Config config = new RateLimitingGatewayFilter.Config();
        config.setCapacity(capacity);
        config.setRefillTokens(capacity);
        config.setRefillPeriod(period);
        return config;
    }
}
