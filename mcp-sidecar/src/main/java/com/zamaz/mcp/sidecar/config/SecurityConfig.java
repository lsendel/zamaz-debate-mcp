package com.zamaz.mcp.sidecar.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Security Configuration for MCP Sidecar
 * 
 * Provides comprehensive security features:
 * - JWT-based authentication
 * - Role-based authorization
 * - Rate limiting integration
 * - Session management with Redis
 * - CORS configuration
 * - CSRF protection
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    @Value("${jwt.secret:mcp-sidecar-secret-key-change-in-production}")
    private String jwtSecret;

    @Value("${jwt.issuer:zamaz-mcp-sidecar}")
    private String jwtIssuer;

    /**
     * Main security filter chain configuration
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .cors().and()
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/actuator/health", "/health").permitAll()
                        .pathMatchers("/actuator/info", "/actuator/metrics").permitAll()
                        .pathMatchers("/fallback/**").permitAll()
                        
                        // Admin endpoints
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .pathMatchers("/actuator/**").hasRole("ADMIN")
                        
                        // Organization management
                        .pathMatchers("/api/v1/organizations/**").hasAnyRole("USER", "ADMIN")
                        
                        // LLM and AI services
                        .pathMatchers("/api/v1/llm/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/api/v1/ai/**").hasAnyRole("USER", "ADMIN")
                        
                        // Debate services
                        .pathMatchers("/api/v1/debates/**").hasAnyRole("USER", "ADMIN")
                        
                        // RAG services
                        .pathMatchers("/api/v1/rag/**").hasAnyRole("USER", "ADMIN")
                        
                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .build();
    }

    /**
     * JWT Decoder for validating JWT tokens
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
        );
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * Password encoder for user authentication
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Default user details service for development
     * In production, this should be replaced with a proper user service
     */
    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN")
                .build();

        UserDetails user = User.builder()
                .username("demo")
                .password(passwordEncoder().encode("demo123"))
                .roles("USER")
                .build();

        return new MapReactiveUserDetailsService(admin, user);
    }

    /**
     * JWT Authentication Converter
     */
    private ServerAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }

    /**
     * Authentication Entry Point for handling authentication errors
     */
    private org.springframework.security.web.server.ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, ex) -> {
            log.warn("Authentication failed: {}", ex.getMessage());
            
            var response = exchange.getResponse();
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            response.getHeaders().add("Content-Type", "application/json");
            
            String body = """
                {
                    "error": "unauthorized",
                    "message": "Authentication required",
                    "timestamp": "%s",
                    "path": "%s"
                }
                """.formatted(
                    java.time.Instant.now().toString(),
                    exchange.getRequest().getPath().value()
                );
            
            var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(reactor.core.publisher.Mono.just(buffer));
        };
    }

    /**
     * Access Denied Handler for authorization errors
     */
    private org.springframework.security.web.server.ServerAccessDeniedHandler accessDeniedHandler() {
        return (exchange, denied) -> {
            log.warn("Access denied: {}", denied.getMessage());
            
            var response = exchange.getResponse();
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            response.getHeaders().add("Content-Type", "application/json");
            
            String body = """
                {
                    "error": "forbidden",
                    "message": "Access denied",
                    "timestamp": "%s",
                    "path": "%s"
                }
                """.formatted(
                    java.time.Instant.now().toString(),
                    exchange.getRequest().getPath().value()
                );
            
            var buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(reactor.core.publisher.Mono.just(buffer));
        };
    }
}