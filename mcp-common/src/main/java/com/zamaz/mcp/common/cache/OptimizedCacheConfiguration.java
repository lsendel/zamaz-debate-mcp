package com.zamaz.mcp.common.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Optimized cache configuration with different TTL strategies for different data types.
 * 
 * Cache Strategy Categories:
 * - Ultra-short (1 minute): Real-time data that changes frequently
 * - Short (5 minutes): Frequently changing operational data
 * - Medium (1 hour): Stable operational data
 * - Long (24 hours): Static or completed data
 * - Very long (7 days): Template and configuration data
 */
@Configuration
@EnableCaching
public class OptimizedCacheConfiguration {

    /**
     * Create optimized cache manager with tiered TTL strategies
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // Default 1 hour TTL
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Cache-specific configurations with optimized TTL strategies
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // ULTRA-SHORT TTL (1 minute) - Real-time data
        configureCacheWithTtl(cacheConfigurations, defaultConfig, Duration.ofMinutes(1),
                "active-debates",           // Currently active debates
                "live-responses",           // Live debate responses
                "websocket-connections",    // Active WebSocket connections
                "realtime-votes",          // Real-time voting data
                "live-comments",           // Live comments during debates
                "active-participants"      // Currently active participants
        );

        // SHORT TTL (5 minutes) - Frequently changing operational data
        configureCacheWithTtl(cacheConfigurations, defaultConfig, Duration.ofMinutes(5),
                "debate-lists",            // Paginated debate lists
                "user-sessions",           // Active user sessions
                "participant-status",      // Participant online status
                "recent-activities",       // Recent user activities
                "organization-stats",      // Organization statistics
                "system-health",           // System health indicators
                "rate-limit-status"        // Rate limiting status
        );

        // MEDIUM TTL (1 hour) - Stable operational data
        configureCacheWithTtl(cacheConfigurations, defaultConfig, Duration.ofHours(1),
                "debates",                 // Individual debate details
                "organizations",           // Organization details
                "users",                   // User profiles
                "participants",            // Participant details
                "rounds",                  // Debate rounds
                "debate-settings",         // Debate configuration
                "llm-providers",           // LLM provider status
                "context-windows"          // Context window data
        );

        // LONG TTL (24 hours) - Static or completed data
        configureCacheWithTtl(cacheConfigurations, defaultConfig, Duration.ofHours(24),
                "debate-results",          // Completed debate results
                "completed-debates",       // Completed debate summaries
                "user-preferences",        // User preference settings
                "organization-config",     // Organization configurations
                "system-config",           // System-wide configurations
                "provider-capabilities",   // LLM provider capabilities
                "analytics-data",          // Analytics and reporting data
                "audit-summaries"          // Audit log summaries
        );

        // VERY LONG TTL (7 days) - Templates and static configurations
        configureCacheWithTtl(cacheConfigurations, defaultConfig, Duration.ofDays(7),
                "templates",               // Debate templates
                "llm-provider-configs",    // LLM provider configurations
                "system-templates",        // System templates
                "email-templates",         // Email message templates
                "validation-rules",        // Business validation rules
                "feature-flags",           // Feature toggle configurations
                "localization-data",       // Internationalization data
                "static-content"           // Static content data
        );

        // CONDITIONAL TTL - Special cases with conditional caching
        configureCacheWithTtl(cacheConfigurations, defaultConfig, Duration.ofDays(30),
                "archived-debates",        // Archived debates (very long retention)
                "historical-analytics",    // Historical analytics data
                "backup-metadata"          // Backup and recovery metadata
        );

        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware() // Enable transaction-aware caching
                .build();
    }

    /**
     * Helper method to configure multiple caches with the same TTL
     */
    private void configureCacheWithTtl(Map<String, RedisCacheConfiguration> cacheConfigurations,
                                      RedisCacheConfiguration defaultConfig,
                                      Duration ttl,
                                      String... cacheNames) {
        RedisCacheConfiguration config = defaultConfig.entryTtl(ttl);
        for (String cacheName : cacheNames) {
            cacheConfigurations.put(cacheName, config);
        }
    }

    /**
     * Cache configuration for development environment with shorter TTLs
     */
    @Bean
    @org.springframework.context.annotation.Profile("development")
    public CacheManager developmentCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)) // Shorter default TTL for development
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> devCacheConfigurations = new HashMap<>();
        
        // Development environment uses shorter TTLs for faster testing
        configureCacheWithTtl(devCacheConfigurations, defaultConfig, Duration.ofSeconds(30),
                "active-debates", "live-responses", "websocket-connections");
        configureCacheWithTtl(devCacheConfigurations, defaultConfig, Duration.ofMinutes(2),
                "debate-lists", "user-sessions", "participant-status");
        configureCacheWithTtl(devCacheConfigurations, defaultConfig, Duration.ofMinutes(5),
                "debates", "organizations", "users");
        configureCacheWithTtl(devCacheConfigurations, defaultConfig, Duration.ofMinutes(10),
                "debate-results", "completed-debates");
        configureCacheWithTtl(devCacheConfigurations, defaultConfig, Duration.ofHours(1),
                "templates", "llm-provider-configs");

        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(devCacheConfigurations)
                .build();
    }

    /**
     * Cache configuration for production environment with optimized TTLs
     */
    @Bean
    @org.springframework.context.annotation.Profile("production")
    public CacheManager productionCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)) // Longer default TTL for production
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> prodCacheConfigurations = new HashMap<>();
        
        // Production environment uses optimized TTLs for performance
        configureCacheWithTtl(prodCacheConfigurations, defaultConfig, Duration.ofMinutes(2),
                "active-debates", "live-responses", "websocket-connections");
        configureCacheWithTtl(prodCacheConfigurations, defaultConfig, Duration.ofMinutes(10),
                "debate-lists", "user-sessions", "participant-status");
        configureCacheWithTtl(prodCacheConfigurations, defaultConfig, Duration.ofHours(2),
                "debates", "organizations", "users");
        configureCacheWithTtl(prodCacheConfigurations, defaultConfig, Duration.ofDays(1),
                "debate-results", "completed-debates");
        configureCacheWithTtl(prodCacheConfigurations, defaultConfig, Duration.ofDays(14),
                "templates", "llm-provider-configs");

        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(prodCacheConfigurations)
                .transactionAware()
                .build();
    }
}