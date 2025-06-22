package com.virtualcompanion.gateway.config;

public class SecurityConfig {
    
    private final JwtAuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .cors(cors -> cors.disable()) // Handled by Gateway
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authenticationManager(authenticationManager)
            .securityContextRepository(securityContextRepository)
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/v1/auth/verify-email").permitAll()
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/password-reset").permitAll()
                .pathMatchers("/health/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/swagger-ui/**").permitAll()
                .pathMatchers("/v3/api-docs/**").permitAll()
                // WebSocket endpoint
                .pathMatchers("/ws/**").permitAll()
                // All other endpoints require authentication
                .anyExchange().authenticated()
            )
            .build();
    }
}
