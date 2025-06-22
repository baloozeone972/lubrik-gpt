package com.virtualcompanion.media.controller;

public class LoggingGatewayFilter extends AbstractGatewayFilterFactory<LoggingGatewayFilter.Config> {

    public LoggingGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!config.isEnabled()) {
                return chain.filter(exchange);
            }

            String requestId = UUID.randomUUID().toString();
            ServerHttpRequest request = exchange.getRequest();

            log.info("Request: {} {} from {} with ID: {}",
                    request.getMethod(),
                    request.getPath(),
                    request.getRemoteAddress(),
                    requestId);

            long startTime = System.currentTimeMillis();

            return chain.filter(exchange)
                    .then(Mono.fromRunnable(() -> {
                        ServerHttpResponse response = exchange.getResponse();
                        long duration = System.currentTimeMillis() - startTime;

                        log.info("Response: {} for request {} in {}ms",
                                response.getStatusCode(),
                                requestId,
                                duration);
                    }));
        };
    }

    public static class Config {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
