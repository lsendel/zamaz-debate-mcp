package com.zamaz.mcp.debateengine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive Redis caching configuration for the unified debate engine.
 * Implements caching strategies for debates, contexts, participants, and analysis results.
 */
@Configuration
@EnableCaching
public class CacheConfiguration implements CachingConfigurer {

    // Cache names
    public static final String DEBATE_CACHE = "debates";
    public static final String DEBATE_LIST_CACHE = "debate-lists";
    public static final String PARTICIPANT_CACHE = "participants";
    public static final String CONTEXT_CACHE = "contexts";
    public static final String MESSAGE_CACHE = "messages";
    public static final String ANALYSIS_CACHE = "analysis";
    public static final String USER_DEBATES_CACHE = "user-debates";
    public static final String ORG_DEBATES_CACHE = "org-debates";
    public static final String ROUND_CACHE = "rounds";
    public static final String RESPONSE_CACHE = "responses";

    private final ObjectMapper objectMapper;

    public CacheConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use Jackson2JsonRedisSerializer for object serialization
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // Set serializers
        template.setDefaultSerializer(jackson2JsonRedisSerializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Debate caches - longer TTL for completed debates
        cacheConfigurations.put(DEBATE_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(DEBATE_LIST_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(USER_DEBATES_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(ORG_DEBATES_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Participant caches - moderate TTL
        cacheConfigurations.put(PARTICIPANT_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Context caches - shorter TTL for active updates
        cacheConfigurations.put(CONTEXT_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put(MESSAGE_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Round and response caches
        cacheConfigurations.put(ROUND_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(RESPONSE_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Analysis cache - longer TTL as results don't change
        cacheConfigurations.put(ANALYSIS_CACHE, defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
                .enableStatistics()
                .disableCreateOnMissingCache();
    }

    @Override
    @Bean
    public KeyGenerator keyGenerator() {
        return new CustomKeyGenerator();
    }

    /**
     * Custom key generator that creates meaningful cache keys
     */
    public static class CustomKeyGenerator implements KeyGenerator {
        @Override
        public Object generate(Object target, Method method, Object... params) {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName()).append(":");
            sb.append(method.getName()).append(":");
            
            // Handle different parameter types
            for (Object param : params) {
                if (param != null) {
                    if (param instanceof Identifiable) {
                        sb.append(((Identifiable) param).getId());
                    } else {
                        sb.append(param.toString());
                    }
                    sb.append(":");
                }
            }
            
            return sb.toString();
        }
    }

    /**
     * Interface for entities that have an ID
     */
    public interface Identifiable {
        Object getId();
    }
}