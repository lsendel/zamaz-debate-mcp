package com.zamaz.mcp.llm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.common.cache.CacheConfiguration;
import com.zamaz.mcp.common.cache.CacheMetricsCollector;
import com.zamaz.mcp.common.cache.CacheService;
import com.zamaz.mcp.common.cache.MonitoredCacheService;
import com.zamaz.mcp.llm.model.CompletionResponse;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

/**
 * Configuration for LLM caching.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.llm.cache")
@Data
public class LlmCacheConfig {

    /**
     * Whether to enable caching.
     */
    private boolean enabled = true;

    /**
     * TTL for cached completions.
     */
    private Duration ttl = Duration.ofHours(24);

    /**
     * TTL for cached completions by model.
     */
    private ModelTtl modelTtl = new ModelTtl();

    /**
     * Creates a cache service for LLM completions.
     *
     * @param redisTemplate the Redis template
     * @param objectMapper the object mapper
     * @param cacheConfiguration the cache configuration
     * @param metricsCollector the metrics collector
     * @return the cache service
     */
    @Bean
    public CacheService<String, CompletionResponse> completionCacheService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            CacheConfiguration cacheConfiguration,
            CacheMetricsCollector metricsCollector) {
        
        if (!enabled || !cacheConfiguration.isEnabled() || !cacheConfiguration.getRedis().isEnabled()) {
            return new NoOpCacheService<>();
        }
        
        String prefix = cacheConfiguration.getRedis().getKeyPrefixes().getLlm();
        var redisCacheService = cacheConfiguration.createRedisCacheService(
                redisTemplate,
                objectMapper,
                CompletionResponse.class,
                prefix
        );
        
        return new MonitoredCacheService<>(
                redisCacheService,
                metricsCollector,
                "llm-completion"
        );
    }

    /**
     * Get the TTL for a specific model.
     *
     * @param model the model name
     * @return the TTL
     */
    public Duration getTtlForModel(String model) {
        if (model == null) {
            return ttl;
        }
        
        if (model.startsWith("claude-")) {
            return modelTtl.getClaude();
        } else if (model.startsWith("gpt-")) {
            return modelTtl.getGpt();
        } else if (model.startsWith("gemini-")) {
            return modelTtl.getGemini();
        } else {
            return ttl;
        }
    }

    /**
     * TTL configuration by model.
     */
    @Data
    public static class ModelTtl {
        private Duration claude = Duration.ofHours(24);
        private Duration gpt = Duration.ofHours(12);
        private Duration gemini = Duration.ofHours(24);
        private Duration llama = Duration.ofHours(48);
    }

    /**
     * No-op implementation of CacheService for when caching is disabled.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    private static class NoOpCacheService<K, V> implements CacheService<K, V> {
        @Override
        public Optional<V> get(K key) {
            return Optional.empty();
        }

        @Override
        public void put(K key, V value) {
            // No-op
        }

        @Override
        public void put(K key, V value, Duration ttl) {
            // No-op
        }

        @Override
        public boolean remove(K key) {
            return false;
        }

        @Override
        public boolean exists(K key) {
            return false;
        }

        @Override
        public void clear() {
            // No-op
        }

        @Override
        public V getOrCompute(K key, CacheValueSupplier<V> supplier) {
            return supplier.get();
        }

        @Override
        public V getOrCompute(K key, CacheValueSupplier<V> supplier, Duration ttl) {
            return supplier.get();
        }
    }
}
