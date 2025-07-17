package com.zamaz.mcp.common.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Collects metrics for cache operations.
 */
@Slf4j
public class CacheMetricsCollector {

    private MeterRegistry meterRegistry;
    private final Map<String, Counter> hitCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> missCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> getTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> putTimers = new ConcurrentHashMap<>();

    /**
     * Set the meter registry.
     *
     * @param meterRegistry the meter registry
     */
    @Autowired
    public void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record a cache hit.
     *
     * @param cacheName the name of the cache
     */
    public void recordHit(String cacheName) {
        if (meterRegistry == null) {
            return;
        }
        
        hitCounters.computeIfAbsent(cacheName, name ->
                Counter.builder("cache.hits")
                        .tag("cache", name)
                        .description("Number of cache hits")
                        .register(meterRegistry)
        ).increment();
    }

    /**
     * Record a cache miss.
     *
     * @param cacheName the name of the cache
     */
    public void recordMiss(String cacheName) {
        if (meterRegistry == null) {
            return;
        }
        
        missCounters.computeIfAbsent(cacheName, name ->
                Counter.builder("cache.misses")
                        .tag("cache", name)
                        .description("Number of cache misses")
                        .register(meterRegistry)
        ).increment();
    }

    /**
     * Record the time taken for a cache get operation.
     *
     * @param cacheName the name of the cache
     * @param timeNanos the time taken in nanoseconds
     */
    public void recordGetTime(String cacheName, long timeNanos) {
        if (meterRegistry == null) {
            return;
        }
        
        getTimers.computeIfAbsent(cacheName, name ->
                Timer.builder("cache.get.time")
                        .tag("cache", name)
                        .description("Time taken for cache get operations")
                        .register(meterRegistry)
        ).record(timeNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Record the time taken for a cache put operation.
     *
     * @param cacheName the name of the cache
     * @param timeNanos the time taken in nanoseconds
     */
    public void recordPutTime(String cacheName, long timeNanos) {
        if (meterRegistry == null) {
            return;
        }
        
        putTimers.computeIfAbsent(cacheName, name ->
                Timer.builder("cache.put.time")
                        .tag("cache", name)
                        .description("Time taken for cache put operations")
                        .register(meterRegistry)
        ).record(timeNanos, TimeUnit.NANOSECONDS);
    }
}
