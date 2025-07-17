package com.zamaz.mcp.common.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Main service for performance monitoring and APM integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringService {
    
    private final MeterRegistry meterRegistry;
    private final PerformanceMetricsCollector metricsCollector;
    private final APMAgentConfig apmConfig;
    
    // Performance tracking
    private final ConcurrentHashMap<String, PerformanceSnapshot> performanceSnapshots = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastAlertTimes = new ConcurrentHashMap<>();
    
    // Thresholds
    private static final double CPU_THRESHOLD = 0.8;
    private static final double MEMORY_THRESHOLD = 0.85;
    private static final long RESPONSE_TIME_THRESHOLD = 5000; // 5 seconds
    private static final int ERROR_RATE_THRESHOLD = 5; // 5%
    
    /**
     * Initialize performance monitoring
     */
    public void initialize() {
        log.info("Initializing performance monitoring service");
        
        // Initialize metrics collection
        metricsCollector.initializeSystemMetrics();
        
        // Initialize APM agent
        apmConfig.initialize();
        
        // Start performance tracking
        startPerformanceTracking();
        
        log.info("Performance monitoring service initialized");
    }
    
    /**
     * Start monitoring method performance
     */
    public Timer.Sample startMethodMonitoring(String className, String methodName) {
        String metricName = "method.execution.time";
        String[] tags = {
            "class", className,
            "method", methodName
        };
        
        return metricsCollector.startTimer(metricName, tags);
    }
    
    /**
     * Stop monitoring method performance
     */
    public void stopMethodMonitoring(Timer.Sample sample, String className, String methodName, 
                                   boolean success, String errorType) {
        String metricName = "method.execution.time";
        String[] tags = {
            "class", className,
            "method", methodName,
            "success", String.valueOf(success),
            "error_type", errorType != null ? errorType : "none"
        };
        
        metricsCollector.stopTimer(sample, metricName, tags);
        
        // Count method calls
        metricsCollector.incrementCounter("method.calls.total", tags);
        
        // Count errors
        if (!success) {
            metricsCollector.incrementCounter("method.errors.total", tags);
        }
    }
    
    /**
     * Monitor method execution with automatic timing
     */
    @Async("monitoringExecutor")
    public <T> CompletableFuture<T> monitorMethodExecution(String className, String methodName, 
                                                         java.util.function.Supplier<T> method) {
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = startMethodMonitoring(className, methodName);
            
            try {
                T result = method.get();
                stopMethodMonitoring(sample, className, methodName, true, null);
                return result;
            } catch (Exception e) {
                stopMethodMonitoring(sample, className, methodName, false, e.getClass().getSimpleName());
                throw e;
            }
        });
    }
    
    /**
     * Record HTTP request performance
     */
    public void recordHttpRequestPerformance(String method, String uri, int status, long duration) {
        metricsCollector.recordHttpRequest(method, uri, status, duration);
        
        // Check for slow requests
        if (duration > RESPONSE_TIME_THRESHOLD) {
            recordSlowRequest(method, uri, duration);
        }
        
        // Check for errors
        if (status >= 400) {
            recordHttpError(method, uri, status);
        }
    }
    
    /**
     * Record database operation performance
     */
    public void recordDatabasePerformance(String operation, String table, boolean success, long duration) {
        metricsCollector.recordDatabaseOperation(operation, table, success, duration);
        
        // Check for slow queries
        if (duration > 1000) { // 1 second threshold
            recordSlowQuery(operation, table, duration);
        }
    }
    
    /**
     * Record cache operation performance
     */
    public void recordCachePerformance(String operation, String cache, boolean hit, long duration) {
        metricsCollector.recordCacheOperation(operation, cache, hit, duration);
    }
    
    /**
     * Record custom application metric
     */
    public void recordCustomMetric(String name, String type, double value, String... tags) {
        metricsCollector.recordBusinessMetric(name, type, value, tags);
    }
    
    /**
     * Get current performance snapshot
     */
    public PerformanceSnapshot getCurrentPerformanceSnapshot() {
        var osMXBean = ManagementFactory.getOperatingSystemMXBean();
        var memoryMXBean = ManagementFactory.getMemoryMXBean();
        var threadMXBean = ManagementFactory.getThreadMXBean();
        
        var heapUsage = memoryMXBean.getHeapMemoryUsage();
        
        return PerformanceSnapshot.builder()
            .timestamp(LocalDateTime.now())
            .cpuUsage(osMXBean.getProcessCpuLoad())
            .memoryUsed(heapUsage.getUsed())
            .memoryMax(heapUsage.getMax())
            .threadCount(threadMXBean.getThreadCount())
            .systemLoad(osMXBean.getSystemLoadAverage())
            .healthScore(metricsCollector.getSystemHealthScore())
            .build();
    }
    
    /**
     * Get performance statistics for a time period
     */
    public PerformanceStatistics getPerformanceStatistics(String period) {
        // In a real implementation, this would query metrics from the time series database
        // For now, return current snapshot as statistics
        PerformanceSnapshot snapshot = getCurrentPerformanceSnapshot();
        
        return PerformanceStatistics.builder()
            .period(period)
            .avgCpuUsage(snapshot.getCpuUsage())
            .maxMemoryUsage(snapshot.getMemoryUsed())
            .avgResponseTime(100.0) // Placeholder
            .errorRate(1.0) // Placeholder
            .throughput(1000.0) // Placeholder
            .availability(99.9) // Placeholder
            .build();
    }
    
    /**
     * Check system health and trigger alerts if needed
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void checkSystemHealth() {
        try {
            PerformanceSnapshot snapshot = getCurrentPerformanceSnapshot();
            
            // Check CPU usage
            if (snapshot.getCpuUsage() > CPU_THRESHOLD) {
                triggerAlert("HIGH_CPU_USAGE", 
                    String.format("CPU usage is %.2f%%, exceeding threshold of %.2f%%", 
                        snapshot.getCpuUsage() * 100, CPU_THRESHOLD * 100));
            }
            
            // Check memory usage
            double memoryUsage = (double) snapshot.getMemoryUsed() / snapshot.getMemoryMax();
            if (memoryUsage > MEMORY_THRESHOLD) {
                triggerAlert("HIGH_MEMORY_USAGE", 
                    String.format("Memory usage is %.2f%%, exceeding threshold of %.2f%%", 
                        memoryUsage * 100, MEMORY_THRESHOLD * 100));
            }
            
            // Check system health score
            if (snapshot.getHealthScore() < 0.5) {
                triggerAlert("LOW_HEALTH_SCORE", 
                    String.format("System health score is %.2f, below acceptable threshold", 
                        snapshot.getHealthScore()));
            }
            
            // Store snapshot for historical analysis
            performanceSnapshots.put(snapshot.getTimestamp().toString(), snapshot);
            
        } catch (Exception e) {
            log.error("Error checking system health", e);
        }
    }
    
    /**
     * Collect application metrics
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void collectApplicationMetrics() {
        try {
            // Collect JVM metrics
            collectJvmMetrics();
            
            // Collect custom application metrics
            collectCustomMetrics();
            
            // Collect performance counters
            collectPerformanceCounters();
            
        } catch (Exception e) {
            log.error("Error collecting application metrics", e);
        }
    }
    
    /**
     * Generate performance report
     */
    public PerformanceReport generatePerformanceReport(String period) {
        try {
            PerformanceStatistics stats = getPerformanceStatistics(period);
            PerformanceSnapshot currentSnapshot = getCurrentPerformanceSnapshot();
            
            return PerformanceReport.builder()
                .period(period)
                .generatedAt(LocalDateTime.now())
                .statistics(stats)
                .currentSnapshot(currentSnapshot)
                .recommendations(generateRecommendations(stats, currentSnapshot))
                .build();
            
        } catch (Exception e) {
            log.error("Error generating performance report", e);
            throw new RuntimeException("Failed to generate performance report", e);
        }
    }
    
    private void startPerformanceTracking() {
        log.info("Starting performance tracking");
        
        // Register custom metrics
        meterRegistry.gauge("system.health.score", this, 
            service -> service.metricsCollector.getSystemHealthScore());
        
        // Start background monitoring
        schedulePerformanceCollection();
    }
    
    private void schedulePerformanceCollection() {
        // Performance collection is handled by @Scheduled methods
        log.debug("Performance collection scheduled");
    }
    
    private void recordSlowRequest(String method, String uri, long duration) {
        String[] tags = {
            "method", method,
            "uri", uri,
            "duration", String.valueOf(duration)
        };
        
        metricsCollector.incrementCounter("http.requests.slow", tags);
        
        log.warn("Slow request detected: {} {} took {}ms", method, uri, duration);
    }
    
    private void recordHttpError(String method, String uri, int status) {
        String[] tags = {
            "method", method,
            "uri", uri,
            "status", String.valueOf(status)
        };
        
        metricsCollector.incrementCounter("http.errors.total", tags);
        
        log.warn("HTTP error recorded: {} {} returned {}", method, uri, status);
    }
    
    private void recordSlowQuery(String operation, String table, long duration) {
        String[] tags = {
            "operation", operation,
            "table", table,
            "duration", String.valueOf(duration)
        };
        
        metricsCollector.incrementCounter("database.queries.slow", tags);
        
        log.warn("Slow query detected: {} on {} took {}ms", operation, table, duration);
    }
    
    private void triggerAlert(String alertType, String message) {
        String key = alertType + ":" + message;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastAlert = lastAlertTimes.get(key);
        
        // Rate limit alerts - only send once per hour for the same issue
        if (lastAlert != null && now.minusHours(1).isBefore(lastAlert)) {
            return;
        }
        
        lastAlertTimes.put(key, now);
        
        // Record alert metric
        metricsCollector.incrementCounter("alerts.triggered", 
            "type", alertType,
            "severity", "warning");
        
        log.warn("ALERT [{}]: {}", alertType, message);
        
        // In a real implementation, this would send alerts to external systems
        // like Slack, PagerDuty, email, etc.
    }
    
    private void collectJvmMetrics() {
        // JVM metrics are automatically collected by the metrics collector
        log.debug("JVM metrics collected");
    }
    
    private void collectCustomMetrics() {
        // Collect application-specific metrics
        metricsCollector.recordGauge("application.uptime", 
            ManagementFactory.getRuntimeMXBean().getUptime());
        
        metricsCollector.recordGauge("application.performance.snapshots.count", 
            performanceSnapshots.size());
    }
    
    private void collectPerformanceCounters() {
        // Collect various performance counters
        PerformanceSnapshot snapshot = getCurrentPerformanceSnapshot();
        
        metricsCollector.recordGauge("performance.cpu.usage", snapshot.getCpuUsage());
        metricsCollector.recordGauge("performance.memory.usage", 
            (double) snapshot.getMemoryUsed() / snapshot.getMemoryMax());
        metricsCollector.recordGauge("performance.thread.count", snapshot.getThreadCount());
        metricsCollector.recordGauge("performance.system.load", snapshot.getSystemLoad());
        metricsCollector.recordGauge("performance.health.score", snapshot.getHealthScore());
    }
    
    private String generateRecommendations(PerformanceStatistics stats, PerformanceSnapshot snapshot) {
        StringBuilder recommendations = new StringBuilder();
        
        if (stats.getAvgCpuUsage() > 0.7) {
            recommendations.append("- Consider scaling up CPU resources or optimizing CPU-intensive operations\n");
        }
        
        if (stats.getMaxMemoryUsage() > (snapshot.getMemoryMax() * 0.8)) {
            recommendations.append("- Memory usage is high, consider increasing heap size or optimizing memory usage\n");
        }
        
        if (stats.getAvgResponseTime() > 1000) {
            recommendations.append("- Response times are high, consider optimizing database queries or caching\n");
        }
        
        if (stats.getErrorRate() > 1.0) {
            recommendations.append("- Error rate is elevated, review error logs and fix underlying issues\n");
        }
        
        if (recommendations.length() == 0) {
            recommendations.append("- System performance is within acceptable parameters\n");
        }
        
        return recommendations.toString();
    }
    
    // Data classes
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PerformanceSnapshot {
        private LocalDateTime timestamp;
        private double cpuUsage;
        private long memoryUsed;
        private long memoryMax;
        private int threadCount;
        private double systemLoad;
        private double healthScore;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PerformanceStatistics {
        private String period;
        private double avgCpuUsage;
        private long maxMemoryUsage;
        private double avgResponseTime;
        private double errorRate;
        private double throughput;
        private double availability;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PerformanceReport {
        private String period;
        private LocalDateTime generatedAt;
        private PerformanceStatistics statistics;
        private PerformanceSnapshot currentSnapshot;
        private String recommendations;
    }
}