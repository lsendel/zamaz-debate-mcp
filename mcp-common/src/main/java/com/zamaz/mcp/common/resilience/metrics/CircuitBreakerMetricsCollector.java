package com.zamaz.mcp.common.resilience.metrics;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and tracks circuit breaker operation metrics for monitoring and analysis.
 */
@Component
public class CircuitBreakerMetricsCollector {
    
    private final Map<String, CircuitBreakerStats> circuitBreakerStats = new ConcurrentHashMap<>();
    
    /**
     * Records a successful circuit breaker execution.
     */
    public void recordSuccessfulExecution(String circuitBreakerName, Duration executionDuration) {
        CircuitBreakerStats stats = circuitBreakerStats.computeIfAbsent(circuitBreakerName, k -> new CircuitBreakerStats());
        stats.recordSuccessfulExecution(executionDuration);
    }
    
    /**
     * Records a failed circuit breaker execution.
     */
    public void recordFailedExecution(String circuitBreakerName, Duration executionDuration, Throwable exception) {
        CircuitBreakerStats stats = circuitBreakerStats.computeIfAbsent(circuitBreakerName, k -> new CircuitBreakerStats());
        stats.recordFailedExecution(executionDuration, exception);
    }
    
    /**
     * Records a circuit breaker state change.
     */
    public void recordStateChange(String circuitBreakerName, CircuitBreaker.State fromState, CircuitBreaker.State toState) {
        CircuitBreakerStats stats = circuitBreakerStats.computeIfAbsent(circuitBreakerName, k -> new CircuitBreakerStats());
        stats.recordStateChange(fromState, toState);
    }
    
    /**
     * Records a call not permitted by circuit breaker (circuit is open).
     */
    public void recordCallNotPermitted(String circuitBreakerName) {
        CircuitBreakerStats stats = circuitBreakerStats.computeIfAbsent(circuitBreakerName, k -> new CircuitBreakerStats());
        stats.recordCallNotPermitted();
    }
    
    /**
     * Records a fallback execution.
     */
    public void recordFallbackExecution(String circuitBreakerName, boolean fallbackSuccessful, Duration fallbackDuration) {
        CircuitBreakerStats stats = circuitBreakerStats.computeIfAbsent(circuitBreakerName, k -> new CircuitBreakerStats());
        stats.recordFallbackExecution(fallbackSuccessful, fallbackDuration);
    }
    
    /**
     * Gets circuit breaker statistics for a specific circuit breaker.
     */
    public CircuitBreakerStats getCircuitBreakerStats(String circuitBreakerName) {
        return circuitBreakerStats.get(circuitBreakerName);
    }
    
    /**
     * Gets all circuit breaker statistics.
     */
    public Map<String, CircuitBreakerStats> getAllCircuitBreakerStats() {
        return new ConcurrentHashMap<>(circuitBreakerStats);
    }
    
    /**
     * Clears all collected metrics.
     */
    public void clearMetrics() {
        circuitBreakerStats.clear();
    }
    
    /**
     * Statistics for a specific circuit breaker operation.
     */
    public static class CircuitBreakerStats {
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong successfulExecutions = new AtomicLong(0);
        private final AtomicLong failedExecutions = new AtomicLong(0);
        private final AtomicLong callsNotPermitted = new AtomicLong(0);
        private final AtomicLong fallbackExecutions = new AtomicLong(0);
        private final AtomicLong successfulFallbacks = new AtomicLong(0);
        private final AtomicLong totalExecutionTimeMs = new AtomicLong(0);
        private final AtomicLong totalFallbackTimeMs = new AtomicLong(0);
        private final AtomicLong stateChanges = new AtomicLong(0);
        
        private volatile Instant lastExecution = Instant.now();
        private volatile Instant lastStateChange = Instant.now();
        private volatile String lastError;
        private volatile CircuitBreaker.State currentState = CircuitBreaker.State.CLOSED;
        private volatile CircuitBreaker.State previousState = CircuitBreaker.State.CLOSED;
        
        public void recordSuccessfulExecution(Duration duration) {
            totalExecutions.incrementAndGet();
            successfulExecutions.incrementAndGet();
            totalExecutionTimeMs.addAndGet(duration.toMillis());
            lastExecution = Instant.now();
        }
        
        public void recordFailedExecution(Duration duration, Throwable exception) {
            totalExecutions.incrementAndGet();
            failedExecutions.incrementAndGet();
            totalExecutionTimeMs.addAndGet(duration.toMillis());
            lastError = exception.getMessage();
            lastExecution = Instant.now();
        }
        
        public void recordStateChange(CircuitBreaker.State fromState, CircuitBreaker.State toState) {
            previousState = fromState;
            currentState = toState;
            stateChanges.incrementAndGet();
            lastStateChange = Instant.now();
        }
        
        public void recordCallNotPermitted() {
            callsNotPermitted.incrementAndGet();
            lastExecution = Instant.now();
        }
        
        public void recordFallbackExecution(boolean successful, Duration duration) {
            fallbackExecutions.incrementAndGet();
            if (successful) {
                successfulFallbacks.incrementAndGet();
            }
            totalFallbackTimeMs.addAndGet(duration.toMillis());
            lastExecution = Instant.now();
        }
        
        // Getters
        public long getTotalExecutions() { return totalExecutions.get(); }
        public long getSuccessfulExecutions() { return successfulExecutions.get(); }
        public long getFailedExecutions() { return failedExecutions.get(); }
        public long getCallsNotPermitted() { return callsNotPermitted.get(); }
        public long getFallbackExecutions() { return fallbackExecutions.get(); }
        public long getSuccessfulFallbacks() { return successfulFallbacks.get(); }
        public long getStateChanges() { return stateChanges.get(); }
        
        public double getSuccessRate() {
            long total = totalExecutions.get();
            return total > 0 ? (double) successfulExecutions.get() / total : 0.0;
        }
        
        public double getFailureRate() {
            long total = totalExecutions.get();
            return total > 0 ? (double) failedExecutions.get() / total : 0.0;
        }
        
        public double getCallNotPermittedRate() {
            long totalCalls = totalExecutions.get() + callsNotPermitted.get();
            return totalCalls > 0 ? (double) callsNotPermitted.get() / totalCalls : 0.0;
        }
        
        public double getFallbackSuccessRate() {
            long totalFallbacks = fallbackExecutions.get();
            return totalFallbacks > 0 ? (double) successfulFallbacks.get() / totalFallbacks : 0.0;
        }
        
        public double getAverageExecutionTimeMs() {
            long executions = totalExecutions.get();
            return executions > 0 ? (double) totalExecutionTimeMs.get() / executions : 0.0;
        }
        
        public double getAverageFallbackTimeMs() {
            long fallbacks = fallbackExecutions.get();
            return fallbacks > 0 ? (double) totalFallbackTimeMs.get() / fallbacks : 0.0;
        }
        
        public Instant getLastExecution() { return lastExecution; }
        public Instant getLastStateChange() { return lastStateChange; }
        public String getLastError() { return lastError; }
        public CircuitBreaker.State getCurrentState() { return currentState; }
        public CircuitBreaker.State getPreviousState() { return previousState; }
        
        /**
         * Calculates circuit breaker health score from 0.0 (unhealthy) to 1.0 (healthy).
         */
        public double getHealthScore() {
            double successWeight = 0.5;
            double availabilityWeight = 0.3;
            double fallbackWeight = 0.2;
            
            double successComponent = getSuccessRate();
            double availabilityComponent = 1.0 - getCallNotPermittedRate();
            double fallbackComponent = getFallbackExecutions() > 0 ? getFallbackSuccessRate() : 1.0;
            
            return successWeight * successComponent + 
                   availabilityWeight * availabilityComponent + 
                   fallbackWeight * fallbackComponent;
        }
    }
}