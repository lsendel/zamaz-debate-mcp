package com.zamaz.mcp.common.resilience.metrics;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and tracks retry operation metrics for monitoring and analysis.
 */
@Component
public class RetryMetricsCollector {
    
    private final Map<String, RetryStats> retryStats = new ConcurrentHashMap<>();
    
    /**
     * Records a retry attempt.
     */
    public void recordRetryAttempt(String retryName, int attemptNumber, Throwable exception) {
        RetryStats stats = retryStats.computeIfAbsent(retryName, k -> new RetryStats());
        stats.recordAttempt(attemptNumber, exception);
    }
    
    /**
     * Records a successful retry operation.
     */
    public void recordRetrySuccess(String retryName, int totalAttempts, Duration totalDuration) {
        RetryStats stats = retryStats.computeIfAbsent(retryName, k -> new RetryStats());
        stats.recordSuccess(totalAttempts, totalDuration);
    }
    
    /**
     * Records a failed retry operation (all attempts exhausted).
     */
    public void recordRetryFailure(String retryName, int totalAttempts, Duration totalDuration, Throwable finalException) {
        RetryStats stats = retryStats.computeIfAbsent(retryName, k -> new RetryStats());
        stats.recordFailure(totalAttempts, totalDuration, finalException);
    }
    
    /**
     * Gets retry statistics for a specific retry name.
     */
    public RetryStats getRetryStats(String retryName) {
        return retryStats.get(retryName);
    }
    
    /**
     * Gets all retry statistics.
     */
    public Map<String, RetryStats> getAllRetryStats() {
        return new ConcurrentHashMap<>(retryStats);
    }
    
    /**
     * Clears all collected metrics.
     */
    public void clearMetrics() {
        retryStats.clear();
    }
    
    /**
     * Statistics for a specific retry operation.
     */
    public static class RetryStats {
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong successfulExecutions = new AtomicLong(0);
        private final AtomicLong failedExecutions = new AtomicLong(0);
        private final AtomicLong totalAttempts = new AtomicLong(0);
        private final AtomicLong totalDurationMs = new AtomicLong(0);
        private volatile Instant lastExecution = Instant.now();
        private volatile String lastError;
        
        public void recordAttempt(int attemptNumber, Throwable exception) {
            totalAttempts.incrementAndGet();
            if (exception != null) {
                lastError = exception.getMessage();
            }
            lastExecution = Instant.now();
        }
        
        public void recordSuccess(int attempts, Duration duration) {
            totalExecutions.incrementAndGet();
            successfulExecutions.incrementAndGet();
            totalDurationMs.addAndGet(duration.toMillis());
            lastExecution = Instant.now();
        }
        
        public void recordFailure(int attempts, Duration duration, Throwable exception) {
            totalExecutions.incrementAndGet();
            failedExecutions.incrementAndGet();
            totalDurationMs.addAndGet(duration.toMillis());
            lastError = exception.getMessage();
            lastExecution = Instant.now();
        }
        
        // Getters
        public long getTotalExecutions() { return totalExecutions.get(); }
        public long getSuccessfulExecutions() { return successfulExecutions.get(); }
        public long getFailedExecutions() { return failedExecutions.get(); }
        public long getTotalAttempts() { return totalAttempts.get(); }
        public double getAverageDurationMs() {
            long executions = totalExecutions.get();
            return executions > 0 ? (double) totalDurationMs.get() / executions : 0.0;
        }
        public double getSuccessRate() {
            long total = totalExecutions.get();
            return total > 0 ? (double) successfulExecutions.get() / total : 0.0;
        }
        public double getAverageAttemptsPerExecution() {
            long executions = totalExecutions.get();
            return executions > 0 ? (double) totalAttempts.get() / executions : 0.0;
        }
        public Instant getLastExecution() { return lastExecution; }
        public String getLastError() { return lastError; }
    }
}