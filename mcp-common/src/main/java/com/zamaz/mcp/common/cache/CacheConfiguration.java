package com.zamaz.mcp.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuration for caching services.
 */
@Configuration
@ConfigurationProperties(prefix = "mcp.cache")
@Data
public class CacheConfiguration {

    /**
     * Default TTL for cached items.
     */
    private Duration defaultTtl = Duration.ofMinutes(30);

    /**
     * Whether to enable caching.
     */
    private boolean enabled = true;

    /**
     * Redis-specific configuration.
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * Creates a Redis template for String keys and values.
     *
     * @param connectionFactory the Redis connection factory
     * @return the Redis template
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Creates a metrics collector for cache operations.
     *
     * @return the cache metrics collector
     */
    @Bean
    public CacheMetricsCollector cacheMetricsCollector() {
        return new CacheMetricsCollector();
    }

    /**
     * Redis-specific configuration properties.
     */
    @Data
    public static class RedisConfig {
        /**
         * Whether to enable Redis caching.
         */
        private boolean enabled = true;

        /**
         * Maximum memory policy.
         */
        private String maxMemoryPolicy = "allkeys-lru";

        /**
         * Key prefixes for different services.
         */
        private KeyPrefixes keyPrefixes = new KeyPrefixes();
    }

    /**
     * Key prefixes for different services.
     */
    @Data
    public static class KeyPrefixes {
        private String llm = "llm:";
        private String context = "ctx:";
        private String debate = "dbt:";
        private String rag = "rag:";
        private String template = "tpl:";
    }

    /**
     * Factory method to create a Redis cache service.
     *
     * @param redisTemplate the Redis template
     * @param objectMapper the object mapper
     * @param valueType the class of the cached value
     * @param prefix the cache key prefix
     * @param <V> the type of the cached value
     * @return the Redis cache service
     */
    public <V> RedisCacheService<String, V> createRedisCacheService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            Class<V> valueType,
            String prefix) {
        
        return new RedisCacheService<>(
                redisTemplate,
                objectMapper,
                valueType,
                defaultTtl,
                prefix
        );
    }
}
