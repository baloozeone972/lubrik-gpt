package com.virtualcompanion.gateway.config;

public class RateLimiterConfig {
    
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Extract user ID from JWT token
            return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getUserId)
                .switchIfEmpty(Mono.just("anonymous"));
        };
    }
    
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
            return Mono.just(ip);
        };
    }
    
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getPath().value()
        );
    }
}
