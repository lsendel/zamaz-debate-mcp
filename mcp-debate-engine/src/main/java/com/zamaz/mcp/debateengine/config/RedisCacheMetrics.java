package com.zamaz.mcp.debateengine.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Aspect for monitoring cache performance metrics.
 * Tracks cache hits, misses, and response times.
 */
@Aspect
@Component
@Slf4j
public class RedisCacheMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Timer cacheOperationTimer;

    public RedisCacheMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.cacheHitCounter = Counter.builder("cache.hits")
                .description("Number of cache hits")
                .tag("cache", "redis")
                .register(meterRegistry);
                
        this.cacheMissCounter = Counter.builder("cache.misses")
                .description("Number of cache misses")
                .tag("cache", "redis")
                .register(meterRegistry);
                
        this.cacheOperationTimer = Timer.builder("cache.operation.duration")
                .description("Cache operation duration")
                .tag("cache", "redis")
                .register(meterRegistry);
    }

    @Around("@annotation(cacheable)")
    public Object measureCacheOperation(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        long startTime = System.nanoTime();
        boolean cacheHit = false;
        
        try {
            Object result = joinPoint.proceed();
            
            // Simple heuristic: if method executes very quickly, it was likely a cache hit
            long duration = System.nanoTime() - startTime;
            cacheHit = duration < TimeUnit.MILLISECONDS.toNanos(5);
            
            if (cacheHit) {
                cacheHitCounter.increment();
                log.trace("Cache HIT for {}.{}", 
                    joinPoint.getTarget().getClass().getSimpleName(), 
                    joinPoint.getSignature().getName());
            } else {
                cacheMissCounter.increment();
                log.trace("Cache MISS for {}.{}", 
                    joinPoint.getTarget().getClass().getSimpleName(), 
                    joinPoint.getSignature().getName());
            }
            
            return result;
        } finally {
            long duration = System.nanoTime() - startTime;
            cacheOperationTimer.record(duration, TimeUnit.NANOSECONDS);
            
            // Record cache-specific metrics
            String cacheName = cacheable.cacheNames().length > 0 ? cacheable.cacheNames()[0] : "default";
            Timer.Sample.stop(
                Timer.start(meterRegistry),
                Timer.builder("cache.operation")
                    .tag("cache_name", cacheName)
                    .tag("operation", joinPoint.getSignature().getName())
                    .tag("hit", String.valueOf(cacheHit))
                    .register(meterRegistry)
            );
        }
    }

    /**
     * Get cache hit ratio
     */
    public double getCacheHitRatio() {
        double hits = cacheHitCounter.count();
        double misses = cacheMissCounter.count();
        double total = hits + misses;
        
        return total > 0 ? hits / total : 0.0;
    }

    /**
     * Reset cache metrics (useful for testing)
     */
    public void resetMetrics() {
        // Note: Micrometer counters cannot be reset directly
        // This would need custom implementation if required
        log.info("Cache metrics reset requested - current hit ratio: {}", getCacheHitRatio());
    }
}