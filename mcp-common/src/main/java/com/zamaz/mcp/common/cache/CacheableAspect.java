package com.zamaz.mcp.common.cache;

import com.zamaz.mcp.common.monitoring.PerformanceMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Aspect for automatic caching with performance monitoring
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheableAspect {

    private final CacheManager cacheManager;
    private final PerformanceMetricsService metricsService;

    /**
     * Custom cacheable annotation with TTL support
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SmartCacheable {
        String value() default "default";
        String key() default "";
        long ttl() default 3600; // Default TTL in seconds
        TimeUnit timeUnit() default TimeUnit.SECONDS;
        boolean condition() default true;
        String unless() default "";
        boolean recordMetrics() default true;
    }

    /**
     * Around advice for caching with metrics
     */
    @Around("@annotation(smartCacheable)")
    public Object cacheWithMetrics(ProceedingJoinPoint joinPoint, SmartCacheable smartCacheable) throws Throwable {
        String cacheName = smartCacheable.value();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Generate cache key
        String cacheKey = generateCacheKey(smartCacheable.key(), className, methodName, joinPoint.getArgs());
        
        long startTime = System.currentTimeMillis();
        boolean cacheHit = false;
        Object result = null;
        
        try {
            // Try to get from cache
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Cache.ValueWrapper cachedValue = cache.get(cacheKey);
                if (cachedValue != null) {
                    result = cachedValue.get();
                    cacheHit = true;
                    log.debug("Cache hit for key: {} in cache: {}", cacheKey, cacheName);
                } else {
                    log.debug("Cache miss for key: {} in cache: {}", cacheKey, cacheName);
                }
            }
            
            // If not in cache, execute method and cache result
            if (result == null) {
                result = joinPoint.proceed();
                
                // Cache the result if cache exists
                if (cache != null && result != null) {
                    cache.put(cacheKey, result);
                    log.debug("Cached result for key: {} in cache: {}", cacheKey, cacheName);
                }
            }
            
            return result;
            
        } finally {
            // Record metrics if enabled
            if (smartCacheable.recordMetrics()) {
                long duration = System.currentTimeMillis() - startTime;
                metricsService.recordCacheMetrics(cacheName, "get", cacheHit, duration);
            }
        }
    }

    /**
     * Generate cache key from method parameters
     */
    private String generateCacheKey(String keyExpression, String className, String methodName, Object[] args) {
        if (!keyExpression.isEmpty()) {
            // Use custom key expression (simplified - could be enhanced with SpEL)
            return keyExpression;
        }
        
        // Generate default key
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(className).append(":").append(methodName);
        
        for (Object arg : args) {
            if (arg != null) {
                keyBuilder.append(":").append(arg.toString());
            }
        }
        
        return keyBuilder.toString();
    }

    /**
     * Cache eviction annotation
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CacheEvict {
        String value();
        String key() default "";
        boolean allEntries() default false;
        boolean recordMetrics() default true;
    }

    /**
     * Around advice for cache eviction
     */
    @Around("@annotation(cacheEvict)")
    public Object evictCache(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        String cacheName = cacheEvict.value();
        long startTime = System.currentTimeMillis();
        
        try {
            // Execute the method first
            Object result = joinPoint.proceed();
            
            // Then evict from cache
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                if (cacheEvict.allEntries()) {
                    cache.clear();
                    log.debug("Cleared entire cache: {}", cacheName);
                } else {
                    String cacheKey = generateCacheKey(
                            cacheEvict.key(),
                            joinPoint.getTarget().getClass().getSimpleName(),
                            joinPoint.getSignature().getName(),
                            joinPoint.getArgs()
                    );
                    cache.evict(cacheKey);
                    log.debug("Evicted key: {} from cache: {}", cacheKey, cacheName);
                }
            }
            
            return result;
            
        } finally {
            // Record metrics
            if (cacheEvict.recordMetrics()) {
                long duration = System.currentTimeMillis() - startTime;
                String operation = cacheEvict.allEntries() ? "clear" : "evict";
                metricsService.recordCacheMetrics(cacheName, operation, true, duration);
            }
        }
    }

    /**
     * Cache put annotation for explicit caching
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CachePut {
        String value();
        String key() default "";
        String condition() default "";
        boolean recordMetrics() default true;
    }

    /**
     * Around advice for cache put
     */
    @Around("@annotation(cachePut)")
    public Object putCache(ProceedingJoinPoint joinPoint, CachePut cachePut) throws Throwable {
        String cacheName = cachePut.value();
        long startTime = System.currentTimeMillis();
        
        try {
            // Execute the method
            Object result = joinPoint.proceed();
            
            // Cache the result
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null && result != null) {
                String cacheKey = generateCacheKey(
                        cachePut.key(),
                        joinPoint.getTarget().getClass().getSimpleName(),
                        joinPoint.getSignature().getName(),
                        joinPoint.getArgs()
                );
                cache.put(cacheKey, result);
                log.debug("Put result for key: {} in cache: {}", cacheKey, cacheName);
            }
            
            return result;
            
        } finally {
            // Record metrics
            if (cachePut.recordMetrics()) {
                long duration = System.currentTimeMillis() - startTime;
                metricsService.recordCacheMetrics(cacheName, "put", true, duration);
            }
        }
    }

    /**
     * Conditional cache annotation
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ConditionalCache {
        String value();
        String key() default "";
        String condition();
        long ttl() default 3600;
        boolean recordMetrics() default true;
    }

    /**
     * Around advice for conditional caching
     */
    @Around("@annotation(conditionalCache)")
    public Object conditionalCache(ProceedingJoinPoint joinPoint, ConditionalCache conditionalCache) throws Throwable {
        String cacheName = conditionalCache.value();
        long startTime = System.currentTimeMillis();
        boolean cacheHit = false;
        
        try {
            // Evaluate condition (simplified - could use SpEL)
            boolean shouldCache = evaluateCondition(conditionalCache.condition(), joinPoint.getArgs());
            
            if (!shouldCache) {
                // Don't use cache, just execute method
                return joinPoint.proceed();
            }
            
            // Try cache first
            String cacheKey = generateCacheKey(
                    conditionalCache.key(),
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(),
                    joinPoint.getArgs()
            );
            
            Cache cache = cacheManager.getCache(cacheName);
            Object result = null;
            
            if (cache != null) {
                Cache.ValueWrapper cachedValue = cache.get(cacheKey);
                if (cachedValue != null) {
                    result = cachedValue.get();
                    cacheHit = true;
                }
            }
            
            if (result == null) {
                result = joinPoint.proceed();
                if (cache != null && result != null) {
                    cache.put(cacheKey, result);
                }
            }
            
            return result;
            
        } finally {
            if (conditionalCache.recordMetrics()) {
                long duration = System.currentTimeMillis() - startTime;
                metricsService.recordCacheMetrics(cacheName, "conditional", cacheHit, duration);
            }
        }
    }

    /**
     * Simple condition evaluation (can be enhanced with SpEL)
     */
    private boolean evaluateCondition(String condition, Object[] args) {
        // Simplified condition evaluation
        // In a real implementation, you'd use Spring's SpEL
        if (condition.equals("true")) return true;
        if (condition.equals("false")) return false;
        if (condition.startsWith("args[") && condition.contains("] != null")) {
            try {
                int argIndex = Integer.parseInt(condition.substring(5, condition.indexOf(']')));
                return args.length > argIndex && args[argIndex] != null;
            } catch (Exception e) {
                return true;
            }
        }
        return true; // Default to true
    }
}