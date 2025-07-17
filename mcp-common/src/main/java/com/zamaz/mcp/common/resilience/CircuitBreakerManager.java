package com.zamaz.mcp.common.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manager for programmatic circuit breaker operations and monitoring.
 * Provides methods to create, configure, and manage circuit breakers at runtime.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerManager {

    private final CircuitBreakerRegistry registry;

    /**
     * Get or create a circuit breaker with the given name
     */
    public CircuitBreaker getOrCreate(String name) {
        return registry.circuitBreaker(name);
    }

    /**
     * Get or create a circuit breaker with custom configuration
     */
    public CircuitBreaker getOrCreate(String name, CircuitBreakerConfig config) {
        return registry.circuitBreaker(name, config);
    }

    /**
     * Create a circuit breaker with custom configuration builder
     */
    public CircuitBreaker createWithConfig(String name, 
                                         float failureRateThreshold,
                                         int slidingWindowSize,
                                         Duration waitDurationInOpenState) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRateThreshold)
            .slidingWindowSize(slidingWindowSize)
            .waitDurationInOpenState(waitDurationInOpenState)
            .build();
        
        return registry.circuitBreaker(name, config);
    }

    /**
     * Get all circuit breaker names
     */
    public Set<String> getAllCircuitBreakerNames() {
        return registry.getAllCircuitBreakers().stream()
            .map(CircuitBreaker::getName)
            .collect(Collectors.toSet());
    }

    /**
     * Get circuit breaker by name
     */
    public CircuitBreaker getCircuitBreaker(String name) {
        return registry.find(name).orElse(null);
    }

    /**
     * Get circuit breaker status
     */
    public CircuitBreakerStatus getStatus(String name) {
        CircuitBreaker cb = getCircuitBreaker(name);
        if (cb == null) {
            return null;
        }

        CircuitBreaker.Metrics metrics = cb.getMetrics();
        
        return CircuitBreakerStatus.builder()
            .name(name)
            .state(cb.getState().toString())
            .failureRate(metrics.getFailureRate())
            .slowCallRate(metrics.getSlowCallRate())
            .numberOfBufferedCalls(metrics.getNumberOfBufferedCalls())
            .numberOfFailedCalls(metrics.getNumberOfFailedCalls())
            .numberOfSlowCalls(metrics.getNumberOfSlowCalls())
            .numberOfSuccessfulCalls(metrics.getNumberOfSuccessfulCalls())
            .build();
    }

    /**
     * Get all circuit breaker statuses
     */
    public Map<String, CircuitBreakerStatus> getAllStatuses() {
        return registry.getAllCircuitBreakers().stream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                cb -> getStatus(cb.getName())
            ));
    }

    /**
     * Reset circuit breaker to closed state
     */
    public void reset(String name) {
        CircuitBreaker cb = getCircuitBreaker(name);
        if (cb != null) {
            cb.reset();
            log.info("Circuit breaker {} reset to CLOSED state", name);
        }
    }

    /**
     * Force circuit breaker to open state
     */
    public void forceOpen(String name) {
        CircuitBreaker cb = getCircuitBreaker(name);
        if (cb != null) {
            cb.transitionToOpenState();
            log.warn("Circuit breaker {} forced to OPEN state", name);
        }
    }

    /**
     * Disable circuit breaker (force to disabled state)
     */
    public void disable(String name) {
        CircuitBreaker cb = getCircuitBreaker(name);
        if (cb != null) {
            cb.transitionToDisabledState();
            log.info("Circuit breaker {} transitioned to DISABLED state", name);
        }
    }

    /**
     * Check if circuit breaker allows call
     */
    public boolean isCallPermitted(String name) {
        CircuitBreaker cb = getCircuitBreaker(name);
        return cb != null && cb.tryAcquirePermission();
    }

    /**
     * Update circuit breaker configuration dynamically
     */
    public void updateConfiguration(String name, CircuitBreakerConfig newConfig) {
        // Remove old circuit breaker
        registry.remove(name);
        
        // Create new one with updated config
        registry.circuitBreaker(name, newConfig);
        
        log.info("Circuit breaker {} configuration updated", name);
    }

    /**
     * Get circuit breaker health percentage
     */
    public float getHealthPercentage(String name) {
        CircuitBreaker cb = getCircuitBreaker(name);
        if (cb == null) {
            return 0.0f;
        }
        
        CircuitBreaker.Metrics metrics = cb.getMetrics();
        int totalCalls = metrics.getNumberOfBufferedCalls();
        
        if (totalCalls == 0) {
            return 100.0f;
        }
        
        int successfulCalls = metrics.getNumberOfSuccessfulCalls();
        return (float) successfulCalls / totalCalls * 100;
    }

    /**
     * Circuit breaker status data class
     */
    @lombok.Data
    @lombok.Builder
    public static class CircuitBreakerStatus {
        private String name;
        private String state;
        private float failureRate;
        private float slowCallRate;
        private int numberOfBufferedCalls;
        private int numberOfFailedCalls;
        private int numberOfSlowCalls;
        private int numberOfSuccessfulCalls;
    }
}