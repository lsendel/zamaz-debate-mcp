package com.zamaz.mcp.sidecar.filter;

import com.zamaz.mcp.sidecar.service.SecurityScanningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Security Scanning Filter for MCP Sidecar
 * 
 * Integrates security scanning into the request processing pipeline
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityScanningFilter implements WebFilter {

    private final SecurityScanningService securityScanningService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Skip security scanning for actuator endpoints
        if (request.getPath().value().startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        
        // Extract client information
        String clientId = getClientId(request);
        String clientIp = getClientIp(request);
        String path = request.getPath().value();
        String method = request.getMethod().toString();
        Map<String, String> headers = extractHeaders(request);
        
        // Check IP reputation first
        return securityScanningService.checkIPReputation(clientIp)
                .flatMap(ipSafe -> {
                    if (!ipSafe) {
                        return blockRequest(exchange, "IP reputation check failed");
                    }
                    
                    // For requests with body, read and scan the payload
                    if (hasBody(request)) {
                        return scanRequestWithBody(exchange, chain, clientId, path, method, headers);
                    } else {
                        return scanRequestWithoutBody(exchange, chain, clientId, path, method, headers);
                    }
                })
                .onErrorResume(error -> {
                    log.error("Security scanning error: {}", error.getMessage());
                    // Continue processing on error (fail open)
                    return chain.filter(exchange);
                });
    }

    /**
     * Scan request without body
     */
    private Mono<Void> scanRequestWithoutBody(ServerWebExchange exchange, WebFilterChain chain,
                                             String clientId, String path, String method, 
                                             Map<String, String> headers) {
        
        return securityScanningService.scanRequest(clientId, path, method, headers, null)
                .flatMap(scanResult -> {
                    if (scanResult.isBlocked()) {
                        return blockRequest(exchange, scanResult.getReason());
                    }
                    return chain.filter(exchange);
                });
    }

    /**
     * Scan request with body
     */
    private Mono<Void> scanRequestWithBody(ServerWebExchange exchange, WebFilterChain chain,
                                          String clientId, String path, String method, 
                                          Map<String, String> headers) {
        
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    
                    String payload = new String(bytes, StandardCharsets.UTF_8);
                    
                    return securityScanningService.scanRequest(clientId, path, method, headers, payload)
                            .flatMap(scanResult -> {
                                if (scanResult.isBlocked()) {
                                    return blockRequest(exchange, scanResult.getReason());
                                }
                                
                                // Create new exchange with the body data
                                ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                                    @Override
                                    public Flux<DataBuffer> getBody() {
                                        return Flux.just(exchange.getResponse().bufferFactory().wrap(bytes));
                                    }
                                };
                                
                                return chain.filter(exchange.mutate().request(decoratedRequest).build());
                            });
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    /**
     * Block request and return security response
     */
    private Mono<Void> blockRequest(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        exchange.getResponse().getHeaders().add("X-Security-Block-Reason", reason);
        
        String responseBody = String.format(
            "{\"error\":\"Security violation detected\",\"reason\":\"%s\",\"timestamp\":\"%s\"}",
            reason, java.time.Instant.now().toString()
        );
        
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Get client ID from request
     */
    private String getClientId(ServerHttpRequest request) {
        // Try to get from header first
        String clientId = request.getHeaders().getFirst("X-Client-ID");
        if (clientId != null) {
            return clientId;
        }
        
        // Try to get from user ID
        String userId = request.getHeaders().getFirst("X-User-ID");
        if (userId != null) {
            return "user:" + userId;
        }
        
        // Fall back to IP address
        return "ip:" + getClientIp(request);
    }

    /**
     * Get client IP address
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null 
            ? request.getRemoteAddress().getAddress().getHostAddress() 
            : "unknown";
    }

    /**
     * Extract headers for security scanning
     */
    private Map<String, String> extractHeaders(ServerHttpRequest request) {
        Map<String, String> headers = new HashMap<>();
        
        request.getHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name, values.get(0));
            }
        });
        
        return headers;
    }

    /**
     * Check if request has body
     */
    private boolean hasBody(ServerHttpRequest request) {
        String method = request.getMethod().toString();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }
}