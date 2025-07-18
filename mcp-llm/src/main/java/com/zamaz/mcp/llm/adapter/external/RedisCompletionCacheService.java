package com.zamaz.mcp.llm.adapter.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.llm.application.port.outbound.CompletionCacheService;
import com.zamaz.mcp.llm.application.port.outbound.LlmProviderGateway;
import com.zamaz.mcp.llm.config.LlmProperties;
import com.zamaz.mcp.llm.domain.model.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis-based implementation of CompletionCacheService.
 * Provides caching for LLM completion responses with TTL management and statistics.
 */
@Slf4j
@Component
public class RedisCompletionCacheService implements CompletionCacheService {

    private static final String CACHE_KEY_PREFIX = "llm:completion:";
    private static final String CACHE_STATS_PREFIX = "llm:cache:stats:";
    private static final String PROVIDER_PATTERN = "llm:completion:*:provider:%s:*";
    private static final String MODEL_PATTERN = "llm:completion:*:provider:%s:model:%s:*";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LlmProperties.CacheConfig cacheConfig;
    private final MessageDigest messageDigest;
    
    // Cache statistics
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    private final AtomicLong estimatedSize = new AtomicLong(0);
    
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter cacheEvictions;

    public RedisCompletionCacheService(
            ReactiveRedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            LlmProperties llmProperties,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheConfig = llmProperties.getCache();
        
        try {
            this.messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
        
        // Initialize metrics
        this.cacheHits = Counter.builder("llm.cache.hits")
                .description("Number of cache hits")
                .register(meterRegistry);
        
        this.cacheMisses = Counter.builder("llm.cache.misses")
                .description("Number of cache misses")
                .register(meterRegistry);
        
        this.cacheEvictions = Counter.builder("llm.cache.evictions")
                .description("Number of cache evictions")
                .register(meterRegistry);
        
        Gauge.builder("llm.cache.size")
                .description("Estimated cache size")
                .register(meterRegistry, this, RedisCompletionCacheService::getEstimatedSize);
        
        if (cacheConfig == null || !cacheConfig.isEnabled()) {
            log.warn("Cache is disabled or not configured");
        }
        
        log.info("RedisCompletionCacheService initialized with TTL: {}", 
                cacheConfig != null ? cacheConfig.getTtl() : "N/A");
    }

    @Override
    public void cacheCompletion(
            String cacheKey,
            LlmProviderGateway.CompletionResponse response,
            Duration ttl
    ) {
        if (cacheConfig == null || !cacheConfig.isEnabled()) {
            log.debug("Caching is disabled, skipping cache operation");
            return;
        }

        try {
            String jsonResponse = objectMapper.writeValueAsString(response);
            String redisKey = CACHE_KEY_PREFIX + cacheKey;
            
            redisTemplate.opsForValue()
                    .set(redisKey, jsonResponse, ttl)
                    .doOnSuccess(result -> {
                        if (Boolean.TRUE.equals(result)) {
                            estimatedSize.incrementAndGet();
                            log.debug("Cached completion response with key: {}", cacheKey);
                        } else {
                            log.warn("Failed to cache completion response with key: {}", cacheKey);
                        }
                    })
                    .doOnError(error -> {
                        log.error("Error caching completion response with key {}: {}", 
                                cacheKey, error.getMessage());
                    })
                    .subscribe();
                    
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize completion response for caching: {}", e.getMessage());
        }
    }

    @Override
    public Optional<LlmProviderGateway.CompletionResponse> getCachedCompletion(String cacheKey) {
        if (cacheConfig == null || !cacheConfig.isEnabled()) {
            log.debug("Caching is disabled, returning empty");
            return Optional.empty();
        }

        try {
            String redisKey = CACHE_KEY_PREFIX + cacheKey;
            
            return redisTemplate.opsForValue()
                    .get(redisKey)
                    .map(jsonResponse -> {
                        try {
                            hitCount.incrementAndGet();
                            cacheHits.increment();
                            log.debug("Cache hit for key: {}", cacheKey);
                            
                            return objectMapper.readValue(jsonResponse, 
                                    LlmProviderGateway.CompletionResponse.class);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to deserialize cached completion response: {}", 
                                    e.getMessage());
                            // Remove corrupted cache entry
                            redisTemplate.delete(redisKey).subscribe();
                            return null;
                        }
                    })
                    .blockOptional()
                    .filter(response -> response != null);
                    
        } catch (Exception e) {
            log.error("Error retrieving cached completion for key {}: {}", 
                    cacheKey, e.getMessage());
        }
        
        missCount.incrementAndGet();
        cacheMisses.increment();
        log.debug("Cache miss for key: {}", cacheKey);
        return Optional.empty();
    }

    @Override
    public String generateCacheKey(CompletionRequest request, ModelName model, ProviderId providerId) {
        // Create a deterministic cache key based on request parameters
        String keyComponents = String.format(
                "provider:%s:model:%s:prompt:%s:maxTokens:%d:temperature:%.2f:streaming:%b:systemMessages:%b",
                providerId.value(),
                model.value(),
                request.getPrompt().value(),
                request.getMaxTokens(),
                request.getTemperature(),
                request.isStreaming(),
                request.requiresSystemMessageSupport()
        );
        
        // Hash the key components to create a shorter, consistent key
        byte[] hash = messageDigest.digest(keyComponents.getBytes(StandardCharsets.UTF_8));
        StringBuilder hashString = new StringBuilder();
        for (byte b : hash) {
            hashString.append(String.format("%02x", b));
        }
        
        return hashString.toString();
    }

    @Override
    public void invalidateProviderCache(ProviderId providerId) {
        if (cacheConfig == null || !cacheConfig.isEnabled()) {
            log.debug("Caching is disabled, skipping invalidation");
            return;
        }

        String pattern = String.format(PROVIDER_PATTERN, providerId.value());
        
        redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .doOnNext(deletedCount -> {
                    if (deletedCount > 0) {
                        evictionCount.addAndGet(deletedCount);
                        cacheEvictions.increment(deletedCount);
                        estimatedSize.addAndGet(-deletedCount);
                        log.info("Invalidated {} cache entries for provider: {}", 
                                deletedCount, providerId);
                    }
                })
                .doOnError(error -> {
                    log.error("Error invalidating cache for provider {}: {}", 
                            providerId, error.getMessage());
                })
                .subscribe();
    }

    @Override
    public void invalidateModelCache(ProviderId providerId, ModelName model) {
        if (cacheConfig == null || !cacheConfig.isEnabled()) {
            log.debug("Caching is disabled, skipping invalidation");
            return;
        }

        String pattern = String.format(MODEL_PATTERN, providerId.value(), model.value());
        
        redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .doOnNext(deletedCount -> {
                    if (deletedCount > 0) {
                        evictionCount.addAndGet(deletedCount);
                        cacheEvictions.increment(deletedCount);
                        estimatedSize.addAndGet(-deletedCount);
                        log.info("Invalidated {} cache entries for provider: {} model: {}", 
                                deletedCount, providerId, model);
                    }
                })
                .doOnError(error -> {
                    log.error("Error invalidating cache for provider {} model {}: {}", 
                            providerId, model, error.getMessage());
                })
                .subscribe();
    }

    @Override
    public void clearAll() {
        if (cacheConfig == null || !cacheConfig.isEnabled()) {
            log.debug("Caching is disabled, skipping clear operation");
            return;
        }

        String pattern = CACHE_KEY_PREFIX + "*";
        
        redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .doOnNext(deletedCount -> {
                    if (deletedCount > 0) {
                        evictionCount.addAndGet(deletedCount);
                        cacheEvictions.increment(deletedCount);
                        estimatedSize.set(0);
                        log.info("Cleared {} cache entries", deletedCount);
                    }
                })
                .doOnError(error -> {
                    log.error("Error clearing cache: {}", error.getMessage());
                })
                .subscribe();
    }

    @Override
    public CacheStats getStats() {
        long hits = hitCount.get();
        long misses = missCount.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total : 0.0;
        
        return new CacheStats(
                hits,
                misses,
                evictionCount.get(),
                hitRate,
                estimatedSize.get()
        );
    }

    private long getEstimatedSize() {
        return estimatedSize.get();
    }

    /**
     * Get the default TTL from configuration.
     */
    public Duration getDefaultTtl() {
        return cacheConfig != null ? cacheConfig.getTtl() : Duration.ofHours(1);
    }

    /**
     * Check if caching is enabled.
     */
    public boolean isCacheEnabled() {
        return cacheConfig != null && cacheConfig.isEnabled();
    }

    /**
     * Perform cache maintenance operations like removing expired entries.
     * This method can be called periodically by a scheduled task.
     */
    public void performMaintenance() {
        if (!isCacheEnabled()) {
            return;
        }

        log.debug("Performing cache maintenance");
        
        // Update estimated size by counting actual keys
        String pattern = CACHE_KEY_PREFIX + "*";
        redisTemplate.keys(pattern)
                .count()
                .doOnNext(count -> {
                    estimatedSize.set(count);
                    log.debug("Updated cache size estimate: {}", count);
                })
                .doOnError(error -> {
                    log.error("Error during cache maintenance: {}", error.getMessage());
                })
                .subscribe();
    }
}