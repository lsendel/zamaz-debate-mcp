package com.zamaz.mcp.common.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.DistributionSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Publisher for custom application metrics
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomMetricsPublisher {
    
    private final MeterRegistry meterRegistry;
    
    // Metric storage
    private final ConcurrentHashMap<String, Counter> customCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> customTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DistributionSummary> customSummaries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> customGauges = new ConcurrentHashMap<>();
    
    /**
     * Publish a counter metric
     */
    public void publishCounter(String name, String description, double value, String... tags) {
        String key = buildKey(name, tags);
        Counter counter = customCounters.computeIfAbsent(key, k -> 
            Counter.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry));
        
        counter.increment(value);
        
        log.debug("Published counter metric: {} = {}", name, value);
    }
    
    /**
     * Publish a counter metric with value 1
     */
    public void publishCounter(String name, String description, String... tags) {
        publishCounter(name, description, 1.0, tags);
    }
    
    /**
     * Publish a gauge metric
     */
    public void publishGauge(String name, String description, double value, String... tags) {
        String key = buildKey(name, tags);
        AtomicLong atomicValue = customGauges.computeIfAbsent(key, k -> {
            AtomicLong atomic = new AtomicLong();
            Gauge.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry, atomic, AtomicLong::doubleValue);
            return atomic;
        });
        
        atomicValue.set(Math.round(value * 1000)); // Store as thousandths for precision
        
        log.debug("Published gauge metric: {} = {}", name, value);
    }
    
    /**
     * Publish a gauge metric with supplier
     */
    public void publishGauge(String name, String description, Supplier<Double> valueSupplier, String... tags) {
        Gauge.builder(name)
            .description(description)
            .tags(tags)
            .register(meterRegistry, valueSupplier, supplier -> supplier.get());
        
        log.debug("Published gauge metric with supplier: {}", name);
    }
    
    /**
     * Publish a timer metric
     */
    public void publishTimer(String name, String description, Duration duration, String... tags) {
        String key = buildKey(name, tags);
        Timer timer = customTimers.computeIfAbsent(key, k -> 
            Timer.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry));
        
        timer.record(duration);
        
        log.debug("Published timer metric: {} = {}ms", name, duration.toMillis());
    }
    
    /**
     * Publish a timer metric with milliseconds
     */
    public void publishTimer(String name, String description, long milliseconds, String... tags) {
        publishTimer(name, description, Duration.ofMillis(milliseconds), tags);
    }
    
    /**
     * Time a code block and publish the result
     */
    public <T> T timeAndPublish(String name, String description, Supplier<T> code, String... tags) {
        String key = buildKey(name, tags);
        Timer timer = customTimers.computeIfAbsent(key, k -> 
            Timer.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry));
        
        return timer.recordCallable(code::get);
    }
    
    /**
     * Time a code block and publish the result (void methods)
     */
    public void timeAndPublish(String name, String description, Runnable code, String... tags) {
        String key = buildKey(name, tags);
        Timer timer = customTimers.computeIfAbsent(key, k -> 
            Timer.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry));
        
        timer.recordCallable(() -> {
            code.run();
            return null;
        });
    }
    
    /**
     * Publish a distribution summary metric
     */
    public void publishSummary(String name, String description, double value, String... tags) {
        String key = buildKey(name, tags);
        DistributionSummary summary = customSummaries.computeIfAbsent(key, k -> 
            DistributionSummary.builder(name)
                .description(description)
                .tags(tags)
                .register(meterRegistry));
        
        summary.record(value);
        
        log.debug("Published summary metric: {} = {}", name, value);
    }
    
    /**
     * Publish business metrics
     */
    public void publishBusinessMetric(String domain, String name, String type, double value, 
                                    String description, String... tags) {
        String metricName = String.format("business.%s.%s", domain, name);
        
        switch (type.toLowerCase()) {
            case "counter" -> publishCounter(metricName, description, value, tags);
            case "gauge" -> publishGauge(metricName, description, value, tags);
            case "timer" -> publishTimer(metricName, description, Math.round(value), tags);
            case "summary" -> publishSummary(metricName, description, value, tags);
            default -> {
                log.warn("Unknown business metric type: {}", type);
                publishGauge(metricName, description, value, tags);
            }
        }
    }
    
    /**
     * Publish user activity metrics
     */
    public void publishUserActivity(String userId, String action, String resource, 
                                  LocalDateTime timestamp, String... additionalTags) {
        String[] tags = combineArrays(
            new String[]{"user_id", userId, "action", action, "resource", resource},
            additionalTags
        );
        
        publishCounter("user.activity.total", "Total user activities", 1.0, tags);
        publishGauge("user.activity.last_seen", "Last user activity timestamp", 
            timestamp.toEpochSecond(java.time.ZoneOffset.UTC), "user_id", userId);
    }
    
    /**
     * Publish system resource metrics
     */
    public void publishSystemResource(String resourceType, String resourceName, 
                                    double used, double available, double utilization) {
        String[] tags = {"resource_type", resourceType, "resource_name", resourceName};
        
        publishGauge("system.resource.used", "Used resource amount", used, tags);
        publishGauge("system.resource.available", "Available resource amount", available, tags);
        publishGauge("system.resource.utilization", "Resource utilization percentage", 
            utilization, tags);
    }
    
    /**
     * Publish API endpoint metrics
     */
    public void publishApiEndpoint(String method, String endpoint, int statusCode, 
                                 long responseTime, long requestSize, long responseSize) {
        String[] tags = {
            "method", method, 
            "endpoint", endpoint, 
            "status_code", String.valueOf(statusCode),
            "status_class", getStatusClass(statusCode)
        };
        
        publishTimer("api.endpoint.response_time", "API endpoint response time", 
            responseTime, tags);
        publishCounter("api.endpoint.requests", "API endpoint request count", 1.0, tags);
        publishSummary("api.endpoint.request_size", "API endpoint request size", 
            requestSize, tags);
        publishSummary("api.endpoint.response_size", "API endpoint response size", 
            responseSize, tags);
    }
    
    /**
     * Publish cache metrics
     */
    public void publishCacheMetrics(String cacheName, long hits, long misses, 
                                  long size, long maxSize, double hitRatio) {
        String[] tags = {"cache_name", cacheName};
        
        publishCounter("cache.hits", "Cache hits", hits, tags);
        publishCounter("cache.misses", "Cache misses", misses, tags);
        publishGauge("cache.size", "Cache size", size, tags);
        publishGauge("cache.max_size", "Cache maximum size", maxSize, tags);
        publishGauge("cache.hit_ratio", "Cache hit ratio", hitRatio, tags);
        publishGauge("cache.utilization", "Cache utilization", 
            maxSize > 0 ? (double) size / maxSize : 0.0, tags);
    }
    
    /**
     * Publish queue metrics
     */
    public void publishQueueMetrics(String queueName, long size, long capacity, 
                                  long enqueued, long dequeued, long processed, long failed) {
        String[] tags = {"queue_name", queueName};
        
        publishGauge("queue.size", "Queue size", size, tags);
        publishGauge("queue.capacity", "Queue capacity", capacity, tags);
        publishGauge("queue.utilization", "Queue utilization", 
            capacity > 0 ? (double) size / capacity : 0.0, tags);
        
        publishCounter("queue.enqueued", "Items enqueued", enqueued, tags);
        publishCounter("queue.dequeued", "Items dequeued", dequeued, tags);
        publishCounter("queue.processed", "Items processed", processed, tags);
        publishCounter("queue.failed", "Items failed", failed, tags);
    }
    
    /**
     * Publish security metrics
     */
    public void publishSecurityMetric(String eventType, String source, String target, 
                                    String result, String... additionalTags) {
        String[] tags = combineArrays(
            new String[]{"event_type", eventType, "source", source, "target", target, "result", result},
            additionalTags
        );
        
        publishCounter("security.events", "Security events", 1.0, tags);
        
        if ("failure".equals(result) || "denied".equals(result)) {
            publishCounter("security.failures", "Security failures", 1.0, tags);
        }
    }
    
    /**
     * Publish custom application health metrics
     */
    public void publishHealthMetric(String component, String healthCheck, 
                                  boolean healthy, double responseTime, String message) {
        String[] tags = {"component", component, "health_check", healthCheck};
        
        publishGauge("health.status", "Health check status", healthy ? 1.0 : 0.0, tags);
        publishTimer("health.check_time", "Health check response time", 
            Math.round(responseTime), tags);
        publishCounter("health.checks", "Health check count", 1.0, tags);
        
        if (!healthy) {
            publishCounter("health.failures", "Health check failures", 1.0, tags);
        }
    }
    
    /**
     * Publish batch job metrics
     */
    public void publishBatchJobMetrics(String jobName, String status, long duration, 
                                     long itemsProcessed, long itemsFailed) {
        String[] tags = {"job_name", jobName, "status", status};
        
        publishTimer("batch.job.duration", "Batch job duration", duration, tags);
        publishCounter("batch.job.executions", "Batch job executions", 1.0, tags);
        publishCounter("batch.job.items.processed", "Batch job items processed", 
            itemsProcessed, tags);
        publishCounter("batch.job.items.failed", "Batch job items failed", 
            itemsFailed, tags);
        
        if (itemsProcessed > 0) {
            double successRate = (double) (itemsProcessed - itemsFailed) / itemsProcessed;
            publishGauge("batch.job.success_rate", "Batch job success rate", 
                successRate, tags);
        }
    }
    
    /**
     * Get metric registry for advanced usage
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
    
    /**
     * Get all custom counter names
     */
    public java.util.Set<String> getCustomCounterNames() {
        return customCounters.keySet();
    }
    
    /**
     * Get all custom timer names
     */
    public java.util.Set<String> getCustomTimerNames() {
        return customTimers.keySet();
    }
    
    /**
     * Get all custom gauge names
     */
    public java.util.Set<String> getCustomGaugeNames() {
        return customGauges.keySet();
    }
    
    /**
     * Get all custom summary names
     */
    public java.util.Set<String> getCustomSummaryNames() {
        return customSummaries.keySet();
    }
    
    /**
     * Clear all custom metrics
     */
    public void clearCustomMetrics() {
        customCounters.clear();
        customTimers.clear();
        customSummaries.clear();
        customGauges.clear();
        
        log.info("All custom metrics cleared");
    }
    
    private String buildKey(String name, String... tags) {
        StringBuilder keyBuilder = new StringBuilder(name);
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                keyBuilder.append(":").append(tags[i]).append("=").append(tags[i + 1]);
            }
        }
        return keyBuilder.toString();
    }
    
    private String getStatusClass(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return "2xx";
        if (statusCode >= 300 && statusCode < 400) return "3xx";
        if (statusCode >= 400 && statusCode < 500) return "4xx";
        if (statusCode >= 500) return "5xx";
        return "1xx";
    }
    
    private String[] combineArrays(String[] array1, String[] array2) {
        if (array2 == null || array2.length == 0) {
            return array1;
        }
        
        String[] result = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }
}