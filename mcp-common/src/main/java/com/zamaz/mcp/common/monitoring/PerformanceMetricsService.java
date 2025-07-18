package com.zamaz.mcp.common.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Service for recording and managing custom performance metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMetricsService {

    private final MeterRegistry meterRegistry;
    
    // Metric collections
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> gaugeValues = new ConcurrentHashMap<>();

    /**
     * Record timing for an operation
     */
    public Timer.Sample startTimer(String metricName, String... tags) {
        Timer timer = getTimer(metricName, tags);
        return Timer.start(meterRegistry);
    }

    /**
     * Stop timer and record the result
     */
    public void stopTimer(Timer.Sample sample, String metricName, String... tags) {
        Timer timer = getTimer(metricName, tags);
        sample.stop(timer);
    }

    /**
     * Record timing for an operation with automatic timing
     */
    public <T> T timeOperation(String metricName, Supplier<T> operation, String... tags) {
        Timer timer = getTimer(metricName, tags);
        return timer.recordCallable(operation::get);
    }

    /**
     * Record timing for void operation
     */
    public void timeOperation(String metricName, Runnable operation, String... tags) {
        Timer timer = getTimer(metricName, tags);
        timer.recordCallable(() -> {
            operation.run();
            return null;
        });
    }

    /**
     * Increment a counter
     */
    public void incrementCounter(String metricName, String... tags) {
        incrementCounter(metricName, 1.0, tags);
    }

    /**
     * Increment a counter by a specific amount
     */
    public void incrementCounter(String metricName, double amount, String... tags) {
        Counter counter = getCounter(metricName, tags);
        counter.increment(amount);
    }

    /**
     * Set gauge value
     */
    public void setGauge(String metricName, long value, String... tags) {
        String key = buildKey(metricName, tags);
        AtomicLong gaugeValue = gaugeValues.computeIfAbsent(key, k -> {
            AtomicLong atomicValue = new AtomicLong(value);
            registerGauge(metricName, atomicValue, tags);
            return atomicValue;
        });
        gaugeValue.set(value);
    }

    /**
     * Record LLM call metrics
     */
    public void recordLlmCall(String provider, String model, boolean success, long durationMs, long tokens) {
        // Record call count and success rate
        incrementCounter("llm.calls.total", "provider", provider, "model", model);
        incrementCounter("llm.calls." + (success ? "success" : "failure"), "provider", provider, "model", model);
        
        // Record latency
        Timer timer = getTimer("llm.calls.duration", "provider", provider, "model", model);
        timer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        // Record token usage
        incrementCounter("llm.tokens.used", tokens, "provider", provider, "model", model);
        
        log.debug("Recorded LLM call metrics: provider={}, model={}, success={}, duration={}ms, tokens={}", 
                provider, model, success, durationMs, tokens);
    }

    /**
     * Record debate metrics
     */
    public void recordDebateMetrics(String status, String model1, String model2, int rounds, long durationMs) {
        incrementCounter("debates.total", "status", status, "model1", model1, "model2", model2);
        incrementCounter("debates.rounds.total", rounds, "model1", model1, "model2", model2);
        
        Timer timer = getTimer("debates.duration", "status", status);
        timer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        log.debug("Recorded debate metrics: status={}, rounds={}, duration={}ms", status, rounds, durationMs);
    }

    /**
     * Record context metrics
     */
    public void recordContextMetrics(String tenantId, String operation, int messageCount, int totalTokens) {
        incrementCounter("context.operations", "tenant", tenantId, "operation", operation);
        
        if (messageCount > 0) {
            setGauge("context.messages.count", messageCount, "tenant", tenantId);
        }
        
        if (totalTokens > 0) {
            setGauge("context.tokens.count", totalTokens, "tenant", tenantId);
        }
        
        log.debug("Recorded context metrics: tenant={}, operation={}, messages={}, tokens={}", 
                tenantId, operation, messageCount, totalTokens);
    }

    /**
     * Record cache metrics
     */
    public void recordCacheMetrics(String cacheName, String operation, boolean hit, long durationMs) {
        incrementCounter("cache.operations", "cache", cacheName, "operation", operation);
        incrementCounter("cache." + (hit ? "hits" : "misses"), "cache", cacheName);
        
        Timer timer = getTimer("cache.operation.duration", "cache", cacheName, "operation", operation);
        timer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Record API endpoint metrics
     */
    public void recordApiMetrics(String endpoint, String method, int status, long durationMs) {
        String statusClass = getStatusClass(status);
        
        incrementCounter("api.requests", "endpoint", endpoint, "method", method, "status", statusClass);
        
        Timer timer = getTimer("api.request.duration", "endpoint", endpoint, "method", method);
        timer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Record error metrics
     */
    public void recordError(String service, String operation, String errorType, String errorMessage) {
        incrementCounter("errors.total", "service", service, "operation", operation, "type", errorType);
        
        log.debug("Recorded error metric: service={}, operation={}, type={}, message={}", 
                service, operation, errorType, errorMessage);
    }

    /**
     * Record rate limit metrics
     */
    public void recordRateLimit(String provider, boolean allowed, long remainingRequests) {
        incrementCounter("rate_limit.checks", "provider", provider, "result", allowed ? "allowed" : "denied");
        setGauge("rate_limit.remaining", remainingRequests, "provider", provider);
    }

    /**
     * Update system health metrics
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void updateSystemMetrics() {
        // JVM metrics
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        setGauge("jvm.memory.used", usedMemory);
        setGauge("jvm.memory.free", freeMemory);
        setGauge("jvm.memory.total", totalMemory);
        
        // Thread metrics
        setGauge("jvm.threads.count", Thread.activeCount());
        
        log.trace("Updated system metrics");
    }

    // Helper methods
    
    private Timer getTimer(String name, String... tags) {
        String key = buildKey(name, tags);
        return timers.computeIfAbsent(key, k -> 
            Timer.builder(name)
                .tags(buildTags(tags))
                .register(meterRegistry));
    }

    private Counter getCounter(String name, String... tags) {
        String key = buildKey(name, tags);
        return counters.computeIfAbsent(key, k -> 
            Counter.builder(name)
                .tags(buildTags(tags))
                .register(meterRegistry));
    }

    private void registerGauge(String name, AtomicLong value, String... tags) {
        Gauge.builder(name)
            .tags(buildTags(tags))
            .register(meterRegistry, value, AtomicLong::get);
    }

    private String buildKey(String name, String... tags) {
        StringBuilder sb = new StringBuilder(name);
        for (String tag : tags) {
            sb.append(":").append(tag);
        }
        return sb.toString();
    }

    private String[] buildTags(String... tags) {
        if (tags.length % 2 != 0) {
            throw new IllegalArgumentException("Tags must be provided in key-value pairs");
        }
        return tags;
    }

    private String getStatusClass(int status) {
        if (status >= 200 && status < 300) return "2xx";
        if (status >= 300 && status < 400) return "3xx";
        if (status >= 400 && status < 500) return "4xx";
        if (status >= 500) return "5xx";
        return "unknown";
    }

    /**
     * Get metrics summary
     */
    public Map<String, Object> getMetricsSummary() {
        return Map.of(
                "timers", timers.size(),
                "counters", counters.size(),
                "gauges", gaugeValues.size(),
                "registeredMetrics", meterRegistry.getMeters().size()
        );
    }

    /**
     * Clear all custom metrics (for testing)
     */
    public void clearMetrics() {
        timers.clear();
        counters.clear();
        gaugeValues.clear();
        log.info("Cleared all custom metrics");
    }
}