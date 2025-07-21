package com.zamaz.mcp.controller.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;
import java.util.List;

/**
 * Cache configuration for agentic flow operations.
 * Uses Caffeine cache for high-performance in-memory caching.
 */
@Configuration
@EnableCaching
public class AgenticFlowCacheConfig {
    
    @Bean
    @Primary
    public CacheManager agenticFlowCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());
        cacheManager.setCacheNames(List.of(
            "agenticFlowConfigurations",
            "agenticFlowResults",
            "flowTypeStatistics",
            "trendingFlows",
            "flowRecommendations"
        ));
        return cacheManager;
    }
    
    @Bean
    public Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(500)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats();
    }
    
    /**
     * Short-lived cache for flow execution results.
     */
    @Bean
    public CacheManager agenticFlowResultCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(200)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
        );
        cacheManager.setCacheNames(List.of("agenticFlowExecutions"));
        return cacheManager;
    }
    
    /**
     * Long-lived cache for analytics data.
     */
    @Bean
    public CacheManager agenticFlowAnalyticsCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .initialCapacity(20)
                .maximumSize(100)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .recordStats()
        );
        cacheManager.setCacheNames(List.of(
            "flowTypeAnalytics",
            "debateAnalytics",
            "organizationAnalytics"
        ));
        return cacheManager;
    }
}