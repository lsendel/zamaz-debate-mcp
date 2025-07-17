package com.zamaz.mcp.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis caching configuration with multiple cache strategies
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(value = "cache.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class CacheConfig {
    
    @Value("${cache.default.ttl:3600}")
    private long defaultTtl;
    
    @Value("${cache.short.ttl:300}")
    private long shortTtl;
    
    @Value("${cache.long.ttl:86400}")
    private long longTtl;
    
    /**
     * Primary cache manager with multiple cache configurations
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        // Configure JSON serialization
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(defaultTtl))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .disableCachingNullValues();
        
        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Short-lived caches (5 minutes)
        cacheConfigurations.put("organizations", defaultConfig.entryTtl(Duration.ofSeconds(shortTtl)));
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofSeconds(shortTtl)));
        cacheConfigurations.put("permissions", defaultConfig.entryTtl(Duration.ofSeconds(shortTtl)));
        
        // Medium-lived caches (1 hour)
        cacheConfigurations.put("debates", defaultConfig.entryTtl(Duration.ofSeconds(defaultTtl)));
        cacheConfigurations.put("contexts", defaultConfig.entryTtl(Duration.ofSeconds(defaultTtl)));
        cacheConfigurations.put("llm-providers", defaultConfig.entryTtl(Duration.ofSeconds(defaultTtl)));
        
        // Long-lived caches (24 hours)
        cacheConfigurations.put("templates", defaultConfig.entryTtl(Duration.ofSeconds(longTtl)));
        cacheConfigurations.put("rag-documents", defaultConfig.entryTtl(Duration.ofSeconds(longTtl)));
        cacheConfigurations.put("embeddings", defaultConfig.entryTtl(Duration.ofSeconds(longTtl)));
        
        // Session cache (30 minutes)
        cacheConfigurations.put("sessions", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Rate limiting cache (1 minute sliding window)
        cacheConfigurations.put("rate-limits", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }
    
    /**
     * Cache manager for volatile data with no persistence
     */
    @Bean
    public CacheManager volatileCacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .build();
    }
    
    /**
     * Custom cache key generator for complex objects
     */
    @Bean
    public CustomCacheKeyGenerator customCacheKeyGenerator() {
        return new CustomCacheKeyGenerator();
    }
    
    /**
     * Cache error handler to prevent cache failures from affecting the application
     */
    @Bean
    public RedisCacheErrorHandler redisCacheErrorHandler() {
        return new RedisCacheErrorHandler();
    }
}