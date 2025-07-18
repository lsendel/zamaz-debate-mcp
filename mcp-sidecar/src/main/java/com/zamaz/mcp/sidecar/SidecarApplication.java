package com.zamaz.mcp.sidecar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * MCP Sidecar Application
 * 
 * Provides enterprise-grade security, authentication, API management,
 * and AI service routing for the Zamaz Debate MCP system.
 * 
 * Key Features:
 * - JWT-based authentication and authorization
 * - Rate limiting and circuit breaker patterns
 * - Intelligent AI service routing and load balancing
 * - Comprehensive monitoring and observability
 * - Redis-based session management
 * - Dynamic configuration management
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.zamaz.mcp.sidecar", "com.zamaz.mcp.common"})
public class SidecarApplication {

    public static void main(String[] args) {
        SpringApplication.run(SidecarApplication.class, args);
    }

    /**
     * Configure API Gateway routes for different services
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Authentication routes (no authentication required)
                .route("auth-service", r -> r.path("/api/v1/auth/**")
                        .uri("${AUTH_SERVICE_URL:http://localhost:8081}"))
                
                // Organization service routes
                .route("organization-service", r -> r.path("/api/v1/organizations/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("organization-service")
                                        .setFallbackUri("forward:/fallback/organization"))
                                .retry(3))
                        .uri("${ORGANIZATION_SERVICE_URL:http://localhost:5005}"))
                
                // LLM service routes with intelligent routing
                .route("llm-service", r -> r.path("/api/v1/llm/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("llm-service")
                                        .setFallbackUri("forward:/fallback/llm"))
                                .retry(2))
                        .uri("${LLM_SERVICE_URL:http://localhost:5002}"))
                
                // Debate controller routes
                .route("debate-controller", r -> r.path("/api/v1/debates/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("debate-controller")
                                        .setFallbackUri("forward:/fallback/debate"))
                                .retry(3))
                        .uri("${CONTROLLER_SERVICE_URL:http://localhost:5013}"))
                
                // RAG service routes
                .route("rag-service", r -> r.path("/api/v1/rag/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("rag-service")
                                        .setFallbackUri("forward:/fallback/rag"))
                                .retry(2))
                        .uri("${RAG_SERVICE_URL:http://localhost:5004}"))
                
                // Health check routes (bypass authentication)
                .route("health-checks", r -> r.path("/actuator/health", "/health")
                        .uri("${GATEWAY_SERVICE_URL:http://localhost:8080}"))
                
                .build();
    }
}