package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.sidecar.service.DistributedCircuitBreakerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Circuit Breaker Controller for MCP Sidecar
 * 
 * Provides REST endpoints for managing circuit breakers
 */
@RestController
@RequestMapping("/api/v1/circuit-breakers")
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerController {

    private final DistributedCircuitBreakerService circuitBreakerService;

    /**
     * Get circuit breaker status
     */
    @GetMapping("/{name}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getCircuitBreakerStatus(@PathVariable String name) {
        return circuitBreakerService.getCircuitBreakerStatus(name)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> log.debug("Circuit breaker status requested for: {}", name))
                .onErrorResume(error -> {
                    log.error("Error getting circuit breaker status for {}: {}", name, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get all circuit breaker statuses
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getAllCircuitBreakerStatuses() {
        return circuitBreakerService.getAllCircuitBreakerStatuses()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("All circuit breaker statuses requested"))
                .onErrorResume(error -> {
                    log.error("Error getting all circuit breaker statuses: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Reset circuit breaker
     */
    @PostMapping("/{name}/reset")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> resetCircuitBreaker(@PathVariable String name) {
        return circuitBreakerService.resetCircuitBreaker(name)
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "status", "Circuit breaker reset",
                    "name", name
                ))))
                .doOnSuccess(response -> log.info("Circuit breaker reset: {}", name))
                .onErrorResume(error -> {
                    log.error("Error resetting circuit breaker {}: {}", name, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Force circuit breaker state
     */
    @PostMapping("/{name}/force-state")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> forceCircuitBreakerState(
            @PathVariable String name,
            @RequestBody ForceStateRequest request) {
        
        try {
            DistributedCircuitBreakerService.State state = 
                DistributedCircuitBreakerService.State.valueOf(request.getState().toUpperCase());
            
            return circuitBreakerService.forceCircuitBreakerState(name, state)
                    .then(Mono.just(ResponseEntity.ok(Map.of(
                        "status", "Circuit breaker state forced",
                        "name", name,
                        "state", state.name()
                    ))))
                    .doOnSuccess(response -> log.info("Circuit breaker state forced: name={}, state={}", name, state))
                    .onErrorResume(error -> {
                        log.error("Error forcing circuit breaker state for {}: {}", name, error.getMessage());
                        return Mono.just(ResponseEntity.internalServerError().build());
                    });
        } catch (IllegalArgumentException e) {
            log.warn("Invalid state specified for circuit breaker {}: {}", name, request.getState());
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    /**
     * Register new circuit breaker
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> registerCircuitBreaker(
            @RequestBody RegisterCircuitBreakerRequest request) {
        
        return Mono.fromRunnable(() -> {
            DistributedCircuitBreakerService.CircuitBreakerConfig config = 
                new DistributedCircuitBreakerService.CircuitBreakerConfig(
                    request.getName(),
                    request.getFailureThreshold(),
                    java.time.Duration.ofSeconds(request.getTimeoutSeconds()),
                    request.getHalfOpenMaxCalls(),
                    request.getSlidingWindowSize(),
                    request.getMinimumNumberOfCalls(),
                    request.getFailureRateThreshold()
                );
            
            circuitBreakerService.registerCircuitBreaker(request.getName(), config);
        })
        .then(Mono.just(ResponseEntity.ok(Map.of(
            "status", "Circuit breaker registered",
            "name", request.getName()
        ))))
        .doOnSuccess(response -> log.info("Circuit breaker registered: {}", request.getName()))
        .onErrorResume(error -> {
            log.error("Error registering circuit breaker {}: {}", request.getName(), error.getMessage());
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getCircuitBreakerHealth() {
        return circuitBreakerService.getAllCircuitBreakerStatuses()
                .map(statuses -> {
                    long openCount = statuses.values().stream()
                            .filter(status -> {
                                Map<String, Object> statusMap = (Map<String, Object>) status;
                                return "OPEN".equals(statusMap.get("state"));
                            })
                            .count();
                    
                    long halfOpenCount = statuses.values().stream()
                            .filter(status -> {
                                Map<String, Object> statusMap = (Map<String, Object>) status;
                                return "HALF_OPEN".equals(statusMap.get("state"));
                            })
                            .count();
                    
                    Map<String, Object> health = Map.of(
                        "status", openCount == 0 ? "UP" : "DEGRADED",
                        "totalCircuitBreakers", statuses.size(),
                        "openCircuitBreakers", openCount,
                        "halfOpenCircuitBreakers", halfOpenCount,
                        "closedCircuitBreakers", statuses.size() - openCount - halfOpenCount
                    );
                    
                    return ResponseEntity.ok(health);
                })
                .doOnSuccess(response -> log.debug("Circuit breaker health check requested"))
                .onErrorResume(error -> {
                    log.error("Error getting circuit breaker health: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get circuit breaker metrics
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getCircuitBreakerMetrics() {
        return circuitBreakerService.getAllCircuitBreakerStatuses()
                .map(statuses -> {
                    Map<String, Object> metrics = new java.util.HashMap<>();
                    
                    // Aggregate metrics
                    long totalCalls = 0;
                    long totalSuccessfulCalls = 0;
                    long totalFailedCalls = 0;
                    double avgFailureRate = 0.0;
                    
                    for (Map.Entry<String, Object> entry : statuses.entrySet()) {
                        Map<String, Object> status = (Map<String, Object>) entry.getValue();
                        totalCalls += ((Number) status.get("totalCalls")).longValue();
                        
                        if (status.containsKey("successfulCalls")) {
                            totalSuccessfulCalls += ((Number) status.get("successfulCalls")).longValue();
                        }
                        if (status.containsKey("failedCalls")) {
                            totalFailedCalls += ((Number) status.get("failedCalls")).longValue();
                        }
                        if (status.containsKey("failureRate")) {
                            avgFailureRate += ((Number) status.get("failureRate")).doubleValue();
                        }
                    }
                    
                    if (!statuses.isEmpty()) {
                        avgFailureRate = avgFailureRate / statuses.size();
                    }
                    
                    metrics.put("totalCalls", totalCalls);
                    metrics.put("totalSuccessfulCalls", totalSuccessfulCalls);
                    metrics.put("totalFailedCalls", totalFailedCalls);
                    metrics.put("overallSuccessRate", totalCalls > 0 ? (double) totalSuccessfulCalls / totalCalls : 0.0);
                    metrics.put("overallFailureRate", totalCalls > 0 ? (double) totalFailedCalls / totalCalls : 0.0);
                    metrics.put("averageFailureRate", avgFailureRate);
                    metrics.put("circuitBreakerDetails", statuses);
                    
                    return ResponseEntity.ok(metrics);
                })
                .doOnSuccess(response -> log.debug("Circuit breaker metrics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting circuit breaker metrics: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Request DTOs
     */
    public static class ForceStateRequest {
        private String state;
        
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
    }

    public static class RegisterCircuitBreakerRequest {
        private String name;
        private int failureThreshold = 5;
        private int timeoutSeconds = 60;
        private int halfOpenMaxCalls = 3;
        private int slidingWindowSize = 100;
        private int minimumNumberOfCalls = 10;
        private double failureRateThreshold = 0.5;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        public int getHalfOpenMaxCalls() { return halfOpenMaxCalls; }
        public void setHalfOpenMaxCalls(int halfOpenMaxCalls) { this.halfOpenMaxCalls = halfOpenMaxCalls; }
        public int getSlidingWindowSize() { return slidingWindowSize; }
        public void setSlidingWindowSize(int slidingWindowSize) { this.slidingWindowSize = slidingWindowSize; }
        public int getMinimumNumberOfCalls() { return minimumNumberOfCalls; }
        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) { this.minimumNumberOfCalls = minimumNumberOfCalls; }
        public double getFailureRateThreshold() { return failureRateThreshold; }
        public void setFailureRateThreshold(double failureRateThreshold) { this.failureRateThreshold = failureRateThreshold; }
    }
}