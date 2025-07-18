package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.resilience.annotation.CircuitBreaker;
import com.zamaz.mcp.common.resilience.annotation.RateLimiter;
import com.zamaz.mcp.common.resilience.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing and monitoring resilience patterns across the application
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilienceService {

    private final CircuitBreakerManager circuitBreakerManager;

    /**
     * Get health status of all circuit breakers
     */
    public Map<String, CircuitBreakerMetrics> getAllCircuitBreakerMetrics() {
        return circuitBreakerManager.getAllCircuitBreakerNames()
                .spliterator()
                .stream()
                .collect(Collectors.toMap(
                        name -> name,
                        name -> circuitBreakerManager.getMetrics(name)
                ));
    }

    /**
     * Get unhealthy circuit breakers (open or half-open)
     */
    public List<CircuitBreakerMetrics> getUnhealthyCircuitBreakers() {
        return getAllCircuitBreakerMetrics().values().stream()
                .filter(metrics -> !metrics.isHealthy())
                .collect(Collectors.toList());
    }

    /**
     * Force open a circuit breaker (for testing or emergency)
     */
    public void forceOpenCircuitBreaker(String name) {
        log.warn("Force opening circuit breaker: {}", name);
        circuitBreakerManager.openCircuitBreaker(name);
    }

    /**
     * Force close a circuit breaker (for recovery)
     */
    public void forceCloseCircuitBreaker(String name) {
        log.info("Force closing circuit breaker: {}", name);
        circuitBreakerManager.closeCircuitBreaker(name);
    }

    /**
     * Reset all circuit breakers
     */
    public void resetAllCircuitBreakers() {
        log.info("Resetting all circuit breakers");
        circuitBreakerManager.getAllCircuitBreakerNames()
                .forEach(name -> circuitBreakerManager.resetCircuitBreaker(name));
    }

    /**
     * Example method with circuit breaker protection
     */
    @CircuitBreaker(name = "example-service", fallbackMethod = "exampleFallback")
    @Retry(name = "example-service", maxAttempts = 3, waitDurationMs = 1000)
    @RateLimiter(name = "example-service", limitForPeriod = 10)
    public String protectedMethod(String input) {
        log.info("Executing protected method with input: {}", input);
        
        // Simulate potential failure
        if (Math.random() > 0.7) {
            throw new RuntimeException("Random failure for demonstration");
        }
        
        return "Success: " + input;
    }

    /**
     * Fallback method for circuit breaker
     */
    public String exampleFallback(String input, Exception ex) {
        log.warn("Fallback method called for input: {} due to: {}", input, ex.getMessage());
        return "Fallback response for: " + input;
    }

    /**
     * Example method with custom circuit breaker configuration
     */
    @CircuitBreaker(
            name = "custom-circuit-breaker",
            useDefault = false,
            failureRateThreshold = 30.0f,
            slidingWindowSize = 50,
            waitDurationInOpenStateSeconds = 30,
            fallbackMethod = "customFallback"
    )
    public String customProtectedMethod(String input) {
        log.info("Executing custom protected method with input: {}", input);
        
        // Simulate external service call
        if (Math.random() > 0.8) {
            throw new RuntimeException("External service failure");
        }
        
        return "Custom Success: " + input;
    }

    /**
     * Fallback for custom circuit breaker
     */
    public String customFallback(String input, Exception ex) {
        return "Custom fallback for: " + input;
    }
}