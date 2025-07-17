package com.zamaz.mcp.common.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.DistributionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collector for performance metrics and system statistics
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    // System MXBeans
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
    
    // Metrics storage
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Gauge> gauges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DistributionSummary> summaries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> atomicGauges = new ConcurrentHashMap<>();
    
    /**
     * Initialize system metrics collection
     */
    public void initializeSystemMetrics() {
        log.info("Initializing system performance metrics");
        
        // JVM Memory metrics
        registerMemoryMetrics();
        
        // System CPU metrics
        registerCpuMetrics();
        
        // Thread metrics
        registerThreadMetrics();
        
        // Runtime metrics
        registerRuntimeMetrics();
        
        // GC metrics
        registerGcMetrics();
        
        // Class loading metrics
        registerClassLoadingMetrics();
        
        log.info("System performance metrics initialized");
    }
    
    /**
     * Record method execution time
     */
    public Timer.Sample startTimer(String name, String... tags) {
        Timer timer = getOrCreateTimer(name, tags);
        return Timer.start(meterRegistry);
    }
    
    /**
     * Stop timer and record duration
     */
    public void stopTimer(Timer.Sample sample, String name, String... tags) {
        Timer timer = getOrCreateTimer(name, tags);
        sample.stop(timer);
    }
    
    /**
     * Record method execution time with automatic timing
     */
    public <T> T timeMethod(String name, String[] tags, java.util.function.Supplier<T> method) {
        Timer timer = getOrCreateTimer(name, tags);
        return timer.recordCallable(method::get);
    }
    
    /**
     * Record method execution time with automatic timing (void methods)
     */
    public void timeMethod(String name, String[] tags, Runnable method) {
        Timer timer = getOrCreateTimer(name, tags);
        timer.recordCallable(() -> {
            method.run();
            return null;
        });
    }
    
    /**
     * Increment counter
     */
    public void incrementCounter(String name, String... tags) {
        incrementCounter(name, 1.0, tags);
    }
    
    /**
     * Increment counter by amount
     */
    public void incrementCounter(String name, double amount, String... tags) {
        Counter counter = getOrCreateCounter(name, tags);
        counter.increment(amount);
    }
    
    /**
     * Record gauge value
     */
    public void recordGauge(String name, double value, String... tags) {
        String key = buildKey(name, tags);
        AtomicLong atomicValue = atomicGauges.computeIfAbsent(key, k -> {
            AtomicLong atomic = new AtomicLong();
            Gauge.builder(name)
                .tags(tags)
                .register(meterRegistry, atomic, AtomicLong::get);
            return atomic;
        });
        atomicValue.set(Math.round(value));
    }
    
    /**
     * Record distribution summary
     */
    public void recordSummary(String name, double value, String... tags) {
        DistributionSummary summary = getOrCreateSummary(name, tags);
        summary.record(value);
    }
    
    /**
     * Record custom business metric
     */
    public void recordBusinessMetric(String name, String type, double value, String... tags) {
        String metricName = "business." + name;
        
        switch (type.toLowerCase()) {
            case "counter" -> incrementCounter(metricName, value, tags);
            case "gauge" -> recordGauge(metricName, value, tags);
            case "timer" -> {
                Timer timer = getOrCreateTimer(metricName, tags);
                timer.record(Math.round(value), TimeUnit.MILLISECONDS);
            }
            case "summary" -> recordSummary(metricName, value, tags);
            default -> log.warn("Unknown metric type: {}", type);
        }
    }
    
    /**
     * Record HTTP request metrics
     */
    public void recordHttpRequest(String method, String uri, int status, long duration) {
        String[] tags = {
            "method", method,
            "uri", normalizeUri(uri),
            "status", String.valueOf(status)
        };
        
        // Count requests
        incrementCounter("http.requests.total", tags);
        
        // Record duration
        Timer timer = getOrCreateTimer("http.request.duration", tags);
        timer.record(duration, TimeUnit.MILLISECONDS);
        
        // Record response size if available
        recordSummary("http.response.size", duration, tags);
    }
    
    /**
     * Record database operation metrics
     */
    public void recordDatabaseOperation(String operation, String table, boolean success, long duration) {
        String[] tags = {
            "operation", operation,
            "table", table,
            "success", String.valueOf(success)
        };
        
        // Count operations
        incrementCounter("database.operations.total", tags);
        
        // Record duration
        Timer timer = getOrCreateTimer("database.operation.duration", tags);
        timer.record(duration, TimeUnit.MILLISECONDS);
        
        // Record errors
        if (!success) {
            incrementCounter("database.errors.total", tags);
        }
    }
    
    /**
     * Record cache operation metrics
     */
    public void recordCacheOperation(String operation, String cache, boolean hit, long duration) {
        String[] tags = {
            "operation", operation,
            "cache", cache,
            "result", hit ? "hit" : "miss"
        };
        
        // Count operations
        incrementCounter("cache.operations.total", tags);
        
        // Record duration
        Timer timer = getOrCreateTimer("cache.operation.duration", tags);
        timer.record(duration, TimeUnit.MILLISECONDS);
        
        // Record hit ratio
        recordGauge("cache.hit.ratio", hit ? 1.0 : 0.0, "cache", cache);
    }
    
    /**
     * Record security event metrics
     */
    public void recordSecurityEvent(String eventType, String source, String outcome) {
        String[] tags = {
            "event_type", eventType,
            "source", source,
            "outcome", outcome
        };
        
        incrementCounter("security.events.total", tags);
    }
    
    /**
     * Record error metrics
     */
    public void recordError(String errorType, String component, String message) {
        String[] tags = {
            "error_type", errorType,
            "component", component,
            "message", truncateMessage(message)
        };
        
        incrementCounter("errors.total", tags);
    }
    
    /**
     * Get current system health score
     */
    public double getSystemHealthScore() {
        double memoryScore = getMemoryHealthScore();
        double cpuScore = getCpuHealthScore();
        double threadScore = getThreadHealthScore();
        
        return (memoryScore + cpuScore + threadScore) / 3.0;
    }
    
    private void registerMemoryMetrics() {
        // Heap memory
        Gauge.builder("jvm.memory.heap.used")
            .register(meterRegistry, memoryMXBean, 
                bean -> bean.getHeapMemoryUsage().getUsed());
        
        Gauge.builder("jvm.memory.heap.max")
            .register(meterRegistry, memoryMXBean, 
                bean -> bean.getHeapMemoryUsage().getMax());
        
        Gauge.builder("jvm.memory.heap.committed")
            .register(meterRegistry, memoryMXBean, 
                bean -> bean.getHeapMemoryUsage().getCommitted());
        
        // Non-heap memory
        Gauge.builder("jvm.memory.nonheap.used")
            .register(meterRegistry, memoryMXBean, 
                bean -> bean.getNonHeapMemoryUsage().getUsed());
        
        Gauge.builder("jvm.memory.nonheap.max")
            .register(meterRegistry, memoryMXBean, 
                bean -> bean.getNonHeapMemoryUsage().getMax());
    }
    
    private void registerCpuMetrics() {
        // System CPU usage
        Gauge.builder("system.cpu.usage")
            .register(meterRegistry, osMXBean, 
                bean -> bean.getSystemCpuLoad());
        
        // Process CPU usage
        Gauge.builder("process.cpu.usage")
            .register(meterRegistry, osMXBean, 
                bean -> bean.getProcessCpuLoad());
        
        // System load average
        Gauge.builder("system.load.average.1m")
            .register(meterRegistry, osMXBean, 
                bean -> bean.getSystemLoadAverage());
        
        // Available processors
        Gauge.builder("system.cpu.count")
            .register(meterRegistry, osMXBean, 
                bean -> bean.getAvailableProcessors());
    }
    
    private void registerThreadMetrics() {
        // Thread count
        Gauge.builder("jvm.threads.live")
            .register(meterRegistry, threadMXBean, 
                ThreadMXBean::getThreadCount);
        
        // Daemon thread count
        Gauge.builder("jvm.threads.daemon")
            .register(meterRegistry, threadMXBean, 
                ThreadMXBean::getDaemonThreadCount);
        
        // Peak thread count
        Gauge.builder("jvm.threads.peak")
            .register(meterRegistry, threadMXBean, 
                ThreadMXBean::getPeakThreadCount);
        
        // Dead locked threads
        Gauge.builder("jvm.threads.deadlocked")
            .register(meterRegistry, threadMXBean, 
                bean -> {
                    long[] deadlocked = bean.findDeadlockedThreads();
                    return deadlocked != null ? deadlocked.length : 0;
                });
    }
    
    private void registerRuntimeMetrics() {
        // Uptime
        Gauge.builder("jvm.uptime")
            .register(meterRegistry, runtimeMXBean, 
                RuntimeMXBean::getUptime);
        
        // Start time
        Gauge.builder("jvm.start.time")
            .register(meterRegistry, runtimeMXBean, 
                RuntimeMXBean::getStartTime);
    }
    
    private void registerGcMetrics() {
        ManagementFactory.getGarbageCollectorMXBeans().forEach(gcBean -> {
            String gcName = gcBean.getName().replace(" ", "_").toLowerCase();
            
            // GC collection count
            Gauge.builder("jvm.gc.collections")
                .tag("gc", gcName)
                .register(meterRegistry, gcBean, 
                    bean -> bean.getCollectionCount());
            
            // GC collection time
            Gauge.builder("jvm.gc.time")
                .tag("gc", gcName)
                .register(meterRegistry, gcBean, 
                    bean -> bean.getCollectionTime());
        });
    }
    
    private void registerClassLoadingMetrics() {
        var classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        
        // Loaded classes
        Gauge.builder("jvm.classes.loaded")
            .register(meterRegistry, classLoadingMXBean, 
                bean -> bean.getLoadedClassCount());
        
        // Unloaded classes
        Gauge.builder("jvm.classes.unloaded")
            .register(meterRegistry, classLoadingMXBean, 
                bean -> bean.getUnloadedClassCount());
    }
    
    private Counter getOrCreateCounter(String name, String... tags) {
        String key = buildKey(name, tags);
        return counters.computeIfAbsent(key, k -> 
            Counter.builder(name).tags(tags).register(meterRegistry));
    }
    
    private Timer getOrCreateTimer(String name, String... tags) {
        String key = buildKey(name, tags);
        return timers.computeIfAbsent(key, k -> 
            Timer.builder(name).tags(tags).register(meterRegistry));
    }
    
    private DistributionSummary getOrCreateSummary(String name, String... tags) {
        String key = buildKey(name, tags);
        return summaries.computeIfAbsent(key, k -> 
            DistributionSummary.builder(name).tags(tags).register(meterRegistry));
    }
    
    private String buildKey(String name, String... tags) {
        StringBuilder keyBuilder = new StringBuilder(name);
        for (int i = 0; i < tags.length; i += 2) {
            keyBuilder.append(":").append(tags[i]).append("=").append(tags[i + 1]);
        }
        return keyBuilder.toString();
    }
    
    private String normalizeUri(String uri) {
        // Replace path variables with placeholders
        return uri.replaceAll("/\\d+", "/{id}")
                  .replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{uuid}");
    }
    
    private String truncateMessage(String message) {
        if (message == null) return "null";
        return message.length() > 100 ? message.substring(0, 100) + "..." : message;
    }
    
    private double getMemoryHealthScore() {
        var heapUsage = memoryMXBean.getHeapMemoryUsage();
        double usedRatio = (double) heapUsage.getUsed() / heapUsage.getMax();
        
        if (usedRatio < 0.7) return 1.0;
        if (usedRatio < 0.8) return 0.8;
        if (usedRatio < 0.9) return 0.5;
        return 0.2;
    }
    
    private double getCpuHealthScore() {
        double cpuUsage = osMXBean.getProcessCpuLoad();
        
        if (cpuUsage < 0.5) return 1.0;
        if (cpuUsage < 0.7) return 0.8;
        if (cpuUsage < 0.9) return 0.5;
        return 0.2;
    }
    
    private double getThreadHealthScore() {
        int threadCount = threadMXBean.getThreadCount();
        int peakCount = threadMXBean.getPeakThreadCount();
        
        double threadRatio = (double) threadCount / peakCount;
        
        if (threadRatio < 0.7) return 1.0;
        if (threadRatio < 0.8) return 0.8;
        if (threadRatio < 0.9) return 0.5;
        return 0.2;
    }
}