package com.zamaz.mcp.sidecar.interceptor;

import com.zamaz.mcp.sidecar.service.MetricsCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Metrics Interceptor for MCP Sidecar
 * 
 * Automatically collects metrics for all incoming requests:
 * - Request duration
 * - Response status codes
 * - Endpoint usage
 * - Error rates
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsInterceptor implements WebFilter {

    private final MetricsCollectorService metricsCollectorService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Instant startTime = Instant.now();
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().toString();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = Duration.between(startTime, Instant.now()).toMillis();
                    int statusCode = exchange.getResponse().getStatusCode() != null 
                        ? exchange.getResponse().getStatusCode().value() 
                        : 500;

                    // Record the request metrics
                    metricsCollectorService.recordRequest(
                        normalizeEndpoint(path),
                        method,
                        duration,
                        statusCode
                    );

                    // Record authentication metrics if it's an auth endpoint
                    if (path.startsWith("/api/v1/auth/")) {
                        recordAuthMetrics(exchange, statusCode);
                    }

                    // Record rate limiting metrics if rate limited
                    if (statusCode == 429) {
                        recordRateLimitMetrics(exchange, path);
                    }
                });
    }

    /**
     * Normalize endpoint path for metrics
     */
    private String normalizeEndpoint(String path) {
        if (path == null) return "unknown";
        
        // Remove IDs and version numbers to reduce cardinality
        path = path.replaceAll("/v\\d+", "");
        path = path.replaceAll("/[0-9a-fA-F-]{36}", "/{id}");
        path = path.replaceAll("/\\d+", "/{id}");
        
        // Group similar endpoints
        if (path.startsWith("/api/v1/auth")) return "/auth";
        if (path.startsWith("/api/v1/organizations")) return "/organizations";
        if (path.startsWith("/api/v1/debates")) return "/debates";
        if (path.startsWith("/api/v1/llm")) return "/llm";
        if (path.startsWith("/api/v1/rag")) return "/rag";
        if (path.startsWith("/actuator")) return "/actuator";
        if (path.startsWith("/fallback")) return "/fallback";
        
        return path;
    }

    /**
     * Record authentication metrics
     */
    private void recordAuthMetrics(ServerWebExchange exchange, int statusCode) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        String authMethod = determineAuthMethod(exchange);
        boolean success = statusCode >= 200 && statusCode < 300;
        
        metricsCollectorService.recordAuthentication(
            userId != null ? userId : "anonymous",
            success,
            authMethod
        );
    }

    /**
     * Record rate limiting metrics
     */
    private void recordRateLimitMetrics(ServerWebExchange exchange, String path) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        String rateLimitType = exchange.getResponse().getHeaders().getFirst("X-Rate-Limit-Type");
        
        metricsCollectorService.recordRateLimitHit(
            userId != null ? userId : "anonymous",
            normalizeEndpoint(path),
            rateLimitType != null ? rateLimitType : "unknown"
        );
    }

    /**
     * Determine authentication method from request
     */
    private String determineAuthMethod(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        String path = exchange.getRequest().getPath().value();
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return "jwt";
        }
        
        if (path.contains("/login")) {
            return "credentials";
        }
        
        if (path.contains("/refresh")) {
            return "refresh_token";
        }
        
        return "unknown";
    }
}