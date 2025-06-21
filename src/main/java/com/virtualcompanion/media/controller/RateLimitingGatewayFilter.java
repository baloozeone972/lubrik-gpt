package com.virtualcompanion.media.controller;

public class RateLimitingGatewayFilter extends AbstractGatewayFilterFactory<RateLimitingGatewayFilter.Config> {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId == null) {
                userId = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }

            Bucket bucket = buckets.computeIfAbsent(userId, k -> createBucket(config));

            if (bucket.tryConsume(1)) {
                return chain.filter(exchange);
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("X-Rate-Limit-Retry-After", 
                        String.valueOf(config.getRefillPeriod()));
                return exchange.getResponse().setComplete();
            }
        };
    }

    private Bucket createBucket(Config config) {
        Bandwidth limit = Bandwidth.classic(
                config.getCapacity(),
                Refill.intervally(config.getRefillTokens(), Duration.ofSeconds(config.getRefillPeriod()))
        );
        return Bucket4j.builder().addLimit(limit).build();
    }

    public static class Config {
        private int capacity = 100;
        private int refillTokens = 100;
        private int refillPeriod = 60;

        // Getters and setters
        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
        public int getRefillTokens() { return refillTokens; }
        public void setRefillTokens(int refillTokens) { this.refillTokens = refillTokens; }
        public int getRefillPeriod() { return refillPeriod; }
        public void setRefillPeriod(int refillPeriod) { this.refillPeriod = refillPeriod; }
    }
}
