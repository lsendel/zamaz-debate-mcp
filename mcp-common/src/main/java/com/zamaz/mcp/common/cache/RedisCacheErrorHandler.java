package com.zamaz.mcp.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Error handler for Redis cache operations to prevent cache failures from affecting the application
 */
@Slf4j
public class RedisCacheErrorHandler implements CacheErrorHandler {
    
    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache get error - cache: {}, key: {}, error: {}", 
            cache.getName(), key, exception.getMessage());
    }
    
    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache put error - cache: {}, key: {}, error: {}", 
            cache.getName(), key, exception.getMessage());
    }
    
    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache evict error - cache: {}, key: {}, error: {}", 
            cache.getName(), key, exception.getMessage());
    }
    
    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache clear error - cache: {}, error: {}", 
            cache.getName(), exception.getMessage());
    }
}