package com.zamaz.mcp.common.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Application performance monitoring component
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationPerformanceMonitor {
    
    private final MeterRegistry meterRegistry;
    private final PerformanceMetricsCollector metricsCollector;
    
    // Performance tracking
    private final ConcurrentHashMap<String, PerformanceStats> methodStats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Long>> responseTimeSamples = new ConcurrentHashMap<>();
    
    // Slow operation tracking
    private final ConcurrentHashMap<String, SlowOperationInfo> slowOperations = new ConcurrentHashMap<>();
    private final Queue<SlowOperationInfo> recentSlowOperations = new ArrayDeque<>();
    
    // Performance thresholds
    private static final long SLOW_METHOD_THRESHOLD = 1000; // 1 second
    private static final long SLOW_REQUEST_THRESHOLD = 5000; // 5 seconds
    private static final long SLOW_DATABASE_THRESHOLD = 500; // 500ms
    private static final int MAX_SLOW_OPERATIONS = 100;
    private static final int MAX_RESPONSE_TIME_SAMPLES = 1000;
    
    /**
     * Record method performance
     */
    public void recordMethodPerformance(String className, String methodName, long duration, 
                                      boolean success, String errorType) {
        String key = className + "." + methodName;
        
        // Update method statistics
        methodStats.computeIfAbsent(key, k -> new PerformanceStats())
            .record(duration, success);
        
        // Record in metrics collector
        metricsCollector.recordBusinessMetric("method.performance", "timer", duration, 
            "class", className, "method", methodName, "success", String.valueOf(success));
        
        // Check for slow methods
        if (duration > SLOW_METHOD_THRESHOLD) {
            recordSlowOperation("METHOD", key, duration, "Method execution");
        }
        
        // Count requests and errors
        requestCounts.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        if (!success) {
            errorCounts.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        }
    }
    
    /**
     * Record HTTP request performance
     */
    public void recordHttpRequestPerformance(String method, String uri, int status, long duration) {
        String key = method + " " + uri;
        
        // Update request statistics
        methodStats.computeIfAbsent(key, k -> new PerformanceStats())
            .record(duration, status < 400);
        
        // Record response time samples
        recordResponseTimeSample(key, duration);
        
        // Check for slow requests
        if (duration > SLOW_REQUEST_THRESHOLD) {
            recordSlowOperation("HTTP_REQUEST", key, duration, 
                String.format("HTTP %s %s returned %d", method, uri, status));
        }
        
        // Count requests and errors
        requestCounts.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        if (status >= 400) {
            errorCounts.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        }
    }
    
    /**
     * Record database query performance
     */
    public void recordDatabaseQueryPerformance(String operation, String table, long duration, 
                                             boolean success, int rowsAffected) {
        String key = operation + " " + table;
        
        // Update query statistics
        methodStats.computeIfAbsent(key, k -> new PerformanceStats())
            .record(duration, success);
        
        // Record in metrics collector
        metricsCollector.recordBusinessMetric("database.query.performance", "timer", duration, 
            "operation", operation, "table", table, "success", String.valueOf(success));
        
        // Check for slow queries
        if (duration > SLOW_DATABASE_THRESHOLD) {
            recordSlowOperation("DATABASE_QUERY", key, duration, 
                String.format("Database %s on %s affected %d rows", operation, table, rowsAffected));
        }
        
        // Count queries and errors
        requestCounts.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        if (!success) {
            errorCounts.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        }
    }
    
    /**
     * Record cache operation performance
     */
    public void recordCacheOperationPerformance(String operation, String cache, boolean hit, long duration) {
        String key = operation + " " + cache;
        
        // Update cache statistics
        methodStats.computeIfAbsent(key, k -> new PerformanceStats())
            .record(duration, true);
        
        // Record in metrics collector
        metricsCollector.recordBusinessMetric("cache.operation.performance", "timer", duration, 
            "operation", operation, "cache", cache, "hit", String.valueOf(hit));
        
        // Count cache operations
        requestCounts.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        
        // Track cache hit ratio
        String hitRatioKey = cache + "_hit_ratio";
        methodStats.computeIfAbsent(hitRatioKey, k -> new PerformanceStats())
            .recordRatio(hit ? 1.0 : 0.0);
    }
    
    /**
     * Get performance summary
     */
    public PerformanceSummary getPerformanceSummary() {
        Map<String, PerformanceMetrics> metrics = new HashMap<>();
        
        methodStats.forEach((key, stats) -> {
            metrics.put(key, PerformanceMetrics.builder()
                .operationName(key)
                .totalCalls(stats.getTotalCalls())
                .successCalls(stats.getSuccessCalls())
                .errorCalls(stats.getErrorCalls())
                .averageResponseTime(stats.getAverageResponseTime())
                .minResponseTime(stats.getMinResponseTime())
                .maxResponseTime(stats.getMaxResponseTime())
                .p95ResponseTime(stats.getP95ResponseTime())
                .p99ResponseTime(stats.getP99ResponseTime())
                .errorRate(stats.getErrorRate())
                .throughput(stats.getThroughput())
                .build());
        });
        
        return PerformanceSummary.builder()
            .timestamp(LocalDateTime.now())
            .metrics(metrics)
            .slowOperations(new ArrayList<>(recentSlowOperations))
            .topSlowOperations(getTopSlowOperations())
            .build();
    }
    
    /**
     * Get top slow operations
     */
    public List<SlowOperationInfo> getTopSlowOperations() {
        return slowOperations.values().stream()
            .sorted(Comparator.comparingLong(SlowOperationInfo::getMaxDuration).reversed())
            .limit(10)
            .toList();
    }
    
    /**
     * Get operation statistics
     */
    public PerformanceStats getOperationStats(String operationName) {
        return methodStats.get(operationName);
    }
    
    /**
     * Get all operation names
     */
    public Set<String> getAllOperationNames() {
        return methodStats.keySet();
    }
    
    /**
     * Get error rate for operation
     */
    public double getErrorRate(String operationName) {
        AtomicLong total = requestCounts.get(operationName);
        AtomicLong errors = errorCounts.get(operationName);
        
        if (total == null || total.get() == 0) {
            return 0.0;
        }
        
        long errorCount = errors != null ? errors.get() : 0;
        return (double) errorCount / total.get();
    }
    
    /**
     * Get throughput for operation
     */
    public double getThroughput(String operationName) {
        PerformanceStats stats = methodStats.get(operationName);
        if (stats == null) {
            return 0.0;
        }
        
        // Calculate throughput based on recent activity
        long recentCalls = stats.getRecentCalls();
        long timeWindow = 60000; // 1 minute
        
        return (double) recentCalls / (timeWindow / 1000.0);
    }
    
    /**
     * Clear performance statistics
     */
    public void clearStatistics() {
        methodStats.clear();
        requestCounts.clear();
        errorCounts.clear();
        responseTimeSamples.clear();
        slowOperations.clear();
        recentSlowOperations.clear();
        
        log.info("Performance statistics cleared");
    }
    
    /**
     * Monitor performance metrics periodically
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorPerformanceMetrics() {
        try {
            // Update performance metrics
            updatePerformanceMetrics();
            
            // Check for performance anomalies
            checkPerformanceAnomalies();
            
            // Clean up old data
            cleanupOldData();
            
        } catch (Exception e) {
            log.error("Error monitoring performance metrics", e);
        }
    }
    
    private void recordSlowOperation(String type, String operation, long duration, String description) {
        String key = type + ":" + operation;
        
        // Update slow operation info
        slowOperations.compute(key, (k, existing) -> {
            if (existing == null) {
                return SlowOperationInfo.builder()
                    .type(type)
                    .operation(operation)
                    .description(description)
                    .firstOccurrence(LocalDateTime.now())
                    .lastOccurrence(LocalDateTime.now())
                    .occurrenceCount(1)
                    .maxDuration(duration)
                    .averageDuration(duration)
                    .build();
            } else {
                existing.setLastOccurrence(LocalDateTime.now());
                existing.setOccurrenceCount(existing.getOccurrenceCount() + 1);
                existing.setMaxDuration(Math.max(existing.getMaxDuration(), duration));
                existing.setAverageDuration(
                    (existing.getAverageDuration() * (existing.getOccurrenceCount() - 1) + duration) / 
                    existing.getOccurrenceCount()
                );
                return existing;
            }
        });
        
        // Add to recent slow operations
        SlowOperationInfo info = SlowOperationInfo.builder()
            .type(type)
            .operation(operation)
            .description(description)
            .firstOccurrence(LocalDateTime.now())
            .lastOccurrence(LocalDateTime.now())
            .occurrenceCount(1)
            .maxDuration(duration)
            .averageDuration(duration)
            .build();
        
        recentSlowOperations.offer(info);
        
        // Limit size of recent operations
        while (recentSlowOperations.size() > MAX_SLOW_OPERATIONS) {
            recentSlowOperations.poll();
        }
        
        log.warn("Slow operation detected: {} - {} took {}ms", type, operation, duration);
    }
    
    private void recordResponseTimeSample(String operation, long duration) {
        responseTimeSamples.computeIfAbsent(operation, k -> new ArrayList<>())
            .add(duration);
        
        // Limit sample size
        List<Long> samples = responseTimeSamples.get(operation);
        if (samples.size() > MAX_RESPONSE_TIME_SAMPLES) {
            samples.removeFirst();
        }
    }
    
    private void updatePerformanceMetrics() {
        // Update metrics for each operation
        methodStats.forEach((operation, stats) -> {
            meterRegistry.gauge("application.performance.average_response_time", 
                io.micrometer.core.instrument.Tags.of("operation", operation), 
                stats.getAverageResponseTime());
            
            meterRegistry.gauge("application.performance.error_rate", 
                io.micrometer.core.instrument.Tags.of("operation", operation), 
                stats.getErrorRate());
            
            meterRegistry.gauge("application.performance.throughput", 
                io.micrometer.core.instrument.Tags.of("operation", operation), 
                stats.getThroughput());
        });
        
        // Update slow operations count
        meterRegistry.gauge("application.performance.slow_operations.count", slowOperations.size());
    }
    
    private void checkPerformanceAnomalies() {
        // Check for performance degradation
        methodStats.forEach((operation, stats) -> {
            if (stats.getErrorRate() > 0.1) { // 10% error rate
                log.warn("High error rate detected for operation {}: {:.2f}%", 
                    operation, stats.getErrorRate() * 100);
                
                metricsCollector.incrementCounter("application.performance.anomalies", 
                    "type", "high_error_rate", "operation", operation);
            }
            
            if (stats.getAverageResponseTime() > SLOW_METHOD_THRESHOLD) {
                log.warn("High response time detected for operation {}: {}ms", 
                    operation, stats.getAverageResponseTime());
                
                metricsCollector.incrementCounter("application.performance.anomalies", 
                    "type", "high_response_time", "operation", operation);
            }
        });
    }
    
    private void cleanupOldData() {
        // Remove old slow operations
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        slowOperations.entrySet().removeIf(entry -> 
            entry.getValue().getLastOccurrence().isBefore(cutoff));
        
        // Clean up response time samples
        responseTimeSamples.forEach((operation, samples) -> {
            if (samples.size() > MAX_RESPONSE_TIME_SAMPLES / 2) {
                samples.clear();
            }
        });
    }
    
    // Data classes
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PerformanceMetrics {
        private String operationName;
        private long totalCalls;
        private long successCalls;
        private long errorCalls;
        private double averageResponseTime;
        private long minResponseTime;
        private long maxResponseTime;
        private double p95ResponseTime;
        private double p99ResponseTime;
        private double errorRate;
        private double throughput;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PerformanceSummary {
        private LocalDateTime timestamp;
        private Map<String, PerformanceMetrics> metrics;
        private List<SlowOperationInfo> slowOperations;
        private List<SlowOperationInfo> topSlowOperations;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class SlowOperationInfo {
        private String type;
        private String operation;
        private String description;
        private LocalDateTime firstOccurrence;
        private LocalDateTime lastOccurrence;
        private int occurrenceCount;
        private long maxDuration;
        private long averageDuration;
    }
    
    public static class PerformanceStats {
        private long totalCalls = 0;
        private long successCalls = 0;
        private long errorCalls = 0;
        private long totalResponseTime = 0;
        private long minResponseTime = Long.MAX_VALUE;
        private long maxResponseTime = 0;
        private final List<Long> responseTimes = new ArrayList<>();
        private long lastUpdateTime = System.currentTimeMillis();
        private long recentCalls = 0;
        private double ratioSum = 0.0;
        private int ratioCount = 0;
        
        public synchronized void record(long duration, boolean success) {
            totalCalls++;
            totalResponseTime += duration;
            
            if (success) {
                successCalls++;
            } else {
                errorCalls++;
            }
            
            minResponseTime = Math.min(minResponseTime, duration);
            maxResponseTime = Math.max(maxResponseTime, duration);
            
            responseTimes.add(duration);
            if (responseTimes.size() > 1000) {
                responseTimes.removeFirst();
            }
            
            // Track recent calls
            long now = System.currentTimeMillis();
            if (now - lastUpdateTime < 60000) { // Within last minute
                recentCalls++;
            } else {
                recentCalls = 1;
                lastUpdateTime = now;
            }
        }
        
        public synchronized void recordRatio(double ratio) {
            ratioSum += ratio;
            ratioCount++;
        }
        
        public synchronized double getAverageResponseTime() {
            return totalCalls > 0 ? (double) totalResponseTime / totalCalls : 0.0;
        }
        
        public synchronized double getErrorRate() {
            return totalCalls > 0 ? (double) errorCalls / totalCalls : 0.0;
        }
        
        public synchronized double getThroughput() {
            return recentCalls / 60.0; // calls per second
        }
        
        public synchronized double getP95ResponseTime() {
            if (responseTimes.isEmpty()) return 0.0;
            
            List<Long> sorted = new ArrayList<>(responseTimes);
            sorted.sort(Long::compareTo);
            
            int index = (int) (sorted.size() * 0.95);
            return sorted.get(Math.min(index, sorted.size() - 1));
        }
        
        public synchronized double getP99ResponseTime() {
            if (responseTimes.isEmpty()) return 0.0;
            
            List<Long> sorted = new ArrayList<>(responseTimes);
            sorted.sort(Long::compareTo);
            
            int index = (int) (sorted.size() * 0.99);
            return sorted.get(Math.min(index, sorted.size() - 1));
        }
        
        public synchronized double getAverageRatio() {
            return ratioCount > 0 ? ratioSum / ratioCount : 0.0;
        }
        
        // Getters
        public long getTotalCalls() { return totalCalls; }
        public long getSuccessCalls() { return successCalls; }
        public long getErrorCalls() { return errorCalls; }
        public long getMinResponseTime() { return minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime; }
        public long getMaxResponseTime() { return maxResponseTime; }
        public long getRecentCalls() { return recentCalls; }
    }
}