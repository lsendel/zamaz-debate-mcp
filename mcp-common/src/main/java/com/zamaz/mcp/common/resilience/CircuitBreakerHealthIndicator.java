package com.zamaz.mcp.common.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.boot.actuator.health.Status;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot Actuator health indicator for circuit breakers.
 * Provides health status based on circuit breaker states.
 */
@Component("circuitBreaker")
@RequiredArgsConstructor
public class CircuitBreakerHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final CircuitBreakerManager circuitBreakerManager;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Status overallStatus = Status.UP;
        int openCircuits = 0;
        int halfOpenCircuits = 0;
        int totalCircuits = 0;

        // Check each circuit breaker
        for (CircuitBreaker circuitBreaker : circuitBreakerRegistry.getAllCircuitBreakers()) {
            totalCircuits++;
            String name = circuitBreaker.getName();
            CircuitBreaker.State state = circuitBreaker.getState();
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

            Map<String, Object> cbDetails = new HashMap<>();
            cbDetails.put("state", state.toString());
            cbDetails.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
            cbDetails.put("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));
            cbDetails.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
            cbDetails.put("failedCalls", metrics.getNumberOfFailedCalls());
            cbDetails.put("slowCalls", metrics.getNumberOfSlowCalls());
            cbDetails.put("successfulCalls", metrics.getNumberOfSuccessfulCalls());
            cbDetails.put("notPermittedCalls", metrics.getNumberOfNotPermittedCalls());
            cbDetails.put("healthPercentage", String.format("%.2f%%", 
                circuitBreakerManager.getHealthPercentage(name)));

            details.put(name, cbDetails);

            // Determine overall health
            switch (state) {
                case OPEN:
                    openCircuits++;
                    overallStatus = Status.DOWN;
                    break;
                case HALF_OPEN:
                    halfOpenCircuits++;
                    if (overallStatus != Status.DOWN) {
                        overallStatus = Status.UNKNOWN;
                    }
                    break;
                case FORCED_OPEN:
                    openCircuits++;
                    overallStatus = Status.DOWN;
                    break;
                case DISABLED:
                    // Don't affect health status
                    break;
                case CLOSED:
                    // Circuit is healthy
                    break;
            }
        }

        // Add summary
        details.put("summary", Map.of(
            "total", totalCircuits,
            "open", openCircuits,
            "halfOpen", halfOpenCircuits,
            "closed", totalCircuits - openCircuits - halfOpenCircuits
        ));

        // Build health response
        Health.Builder builder = new Health.Builder()
            .status(overallStatus)
            .withDetails(details);

        // Add descriptive message
        if (overallStatus == Status.DOWN) {
            builder.withDetail("message", 
                String.format("%d circuit(s) are OPEN - services may be unavailable", openCircuits));
        } else if (overallStatus == Status.UNKNOWN) {
            builder.withDetail("message", 
                String.format("%d circuit(s) are HALF_OPEN - services are recovering", halfOpenCircuits));
        } else {
            builder.withDetail("message", "All circuits are functioning normally");
        }

        return builder.build();
    }
}