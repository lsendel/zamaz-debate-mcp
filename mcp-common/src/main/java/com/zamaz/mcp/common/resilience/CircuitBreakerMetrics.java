package com.zamaz.mcp.common.resilience;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Circuit breaker metrics data transfer object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerMetrics {
    
    private String name;
    private String state;
    private float failureRate;
    private float slowCallRate;
    private int numberOfBufferedCalls;
    private int numberOfFailedCalls;
    private int numberOfSlowCalls;
    private int numberOfSuccessfulCalls;
    private long numberOfNotPermittedCalls;
    
    /**
     * Check if the circuit breaker is healthy
     */
    public boolean isHealthy() {
        return "CLOSED".equals(state) || "HALF_OPEN".equals(state);
    }
    
    /**
     * Check if the circuit breaker is open
     */
    public boolean isOpen() {
        return "OPEN".equals(state);
    }
    
    /**
     * Get success rate percentage
     */
    public float getSuccessRate() {
        return 100.0f - failureRate;
    }
}