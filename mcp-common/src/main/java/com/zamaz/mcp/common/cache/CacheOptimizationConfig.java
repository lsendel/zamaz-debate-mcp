package com.zamaz.mcp.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.TimeoutOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive cache optimization configuration
 * Supports both local (Caffeine) and distributed (Redis) caching
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheOptimizationConfig extends CachingConfigurerSupport {

    @Value("${cache.type:redis}")
    private String cacheType;

    @Value("${cache.redis.host:localhost}")
    private String redisHost;

    @Value("${cache.redis.port:6379}")
    private int redisPort;

    @Value("${cache.redis.password:}")
    private String redisPassword;

    @Value("${cache.redis.ttl:3600}")
    private long defaultTtl;

    @Value("${cache.redis.connection-timeout:2000}")
    private long connectionTimeout;

    @Value("${cache.redis.command-timeout:5000}")
    private long commandTimeout;

    @Value("${cache.caffeine.spec:maximumSize=10000,expireAfterWrite=5m}")
    private String caffeineSpec;

    @Value("${cache.enable-statistics:true}")
    private boolean enableStatistics;

    /**
     * Primary cache manager - Redis for distributed caching
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "cache.type", havingValue = "redis", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultTtl))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Configure specific cache settings
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Context cache - longer TTL
        cacheConfigurations.put("contexts", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // LLM response cache - medium TTL
        cacheConfigurations.put("llm-responses", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // Provider list cache - short TTL
        cacheConfigurations.put("providers", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Debate cache - medium TTL
        cacheConfigurations.put("debates", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // User cache - short TTL
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Organization cache - long TTL
        cacheConfigurations.put("organizations", defaultConfig.entryTtl(Duration.ofHours(12)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        log.info("Redis cache manager configured with {} custom cache configurations", cacheConfigurations.size());
        return cacheManager;
    }

    /**
     * Local cache manager using Caffeine for high-performance local caching
     */
    @Bean
    @ConditionalOnProperty(name = "cache.type", havingValue = "caffeine")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeineSpec(caffeineSpec);
        cacheManager.setAllowNullValues(false);
        
        if (enableStatistics) {
            cacheManager.setCaffeine(Caffeine.newBuilder()
                    .maximumSize(10000)
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .recordStats());
        }
        
        log.info("Caffeine cache manager configured with spec: {}", caffeineSpec);
        return cacheManager;
    }

    /**
     * Redis connection factory with optimized settings
     */
    @Bean
    @ConditionalOnProperty(name = "cache.type", havingValue = "redis", matchIfMissing = true)
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        if (!redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(ClientOptions.builder()
                        .autoReconnect(true)
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(commandTimeout)))
                        .build())
                .commandTimeout(Duration.ofMillis(commandTimeout))
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.setValidateConnection(true);
        
        log.info("Redis connection factory configured for {}:{}", redisHost, redisPort);
        return factory;
    }

    /**
     * Redis template for advanced Redis operations
     */
    @Bean
    @ConditionalOnProperty(name = "cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        
        return template;
    }

    /**
     * Custom key generator for complex cache keys
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append(":");
            sb.append(method.getName());
            for (Object obj : params) {
                if (obj != null) {
                    sb.append(":");
                    sb.append(obj.toString());
                }
            }
            return sb.toString();
        };
    }

    /**
     * Cache error handler for graceful degradation
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error("Cache get error - cache: {}, key: {}", cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.error("Cache put error - cache: {}, key: {}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error("Cache evict error - cache: {}, key: {}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.error("Cache clear error - cache: {}", cache.getName(), exception);
            }
        };
    }

    /**
     * Multi-level cache manager combining local and distributed caching
     */
    @Bean
    @ConditionalOnProperty(name = "cache.type", havingValue = "multi-level")
    public CacheManager multiLevelCacheManager(RedisConnectionFactory connectionFactory) {
        return new MultiLevelCacheManager(caffeineCacheManager(), redisCacheManager(connectionFactory));
    }

    /**
     * Cache warming service for preloading critical data
     */
    @Bean
    public CacheWarmingService cacheWarmingService(CacheManager cacheManager) {
        return new CacheWarmingService(cacheManager);
    }

    /**
     * Cache statistics collector
     */
    @Bean
    @ConditionalOnProperty(name = "cache.enable-statistics", havingValue = "true", matchIfMissing = true)
    public CacheStatisticsCollector cacheStatisticsCollector(CacheManager cacheManager) {
        return new CacheStatisticsCollector(cacheManager);
    }

    /**
     * Specialized cache for LLM responses
     */
    @Bean
    public LoadingCache<String, Object> llmResponseCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build(key -> null); // Loading function returns null, cache used manually
    }

    /**
     * Specialized cache for context windows
     */
    @Bean
    public LoadingCache<String, Object> contextWindowCache() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build(key -> null);
    }

    /**
     * Rate limiting cache
     */
    @Bean
    public LoadingCache<String, Object> rateLimitCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .recordStats()
                .build(key -> null);
    }
}