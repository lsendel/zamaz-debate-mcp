package com.zamaz.mcp.context.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.context.dto.ContextWindowRequest;
import com.zamaz.mcp.context.dto.ContextWindowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching context data in Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContextCacheService {
    
    private static final String CONTEXT_CACHE_PREFIX = "context:";
    private static final String WINDOW_CACHE_PREFIX = "window:";
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${app.context.cache.ttl:3600}")
    private long cacheTtlSeconds;
    
    /**
     * Get a context window from cache.
     */
    public ContextWindowResponse getContextWindow(UUID contextId, ContextWindowRequest request) {
        try {
            String key = buildWindowCacheKey(contextId, request);
            String cached = redisTemplate.opsForValue().get(key);
            
            if (cached != null) {
                return objectMapper.readValue(cached, ContextWindowResponse.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving context window from cache", e);
        }
        return null;
    }
    
    /**
     * Cache a context window.
     */
    public void putContextWindow(UUID contextId, ContextWindowRequest request, ContextWindowResponse response) {
        try {
            String key = buildWindowCacheKey(contextId, request);
            String value = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, value, cacheTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error caching context window", e);
        }
    }
    
    /**
     * Evict all cached data for a context.
     */
    public void evictContext(UUID contextId) {
        try {
            // Delete all keys with this context ID prefix
            String pattern = CONTEXT_CACHE_PREFIX + contextId + "*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            
            pattern = WINDOW_CACHE_PREFIX + contextId + "*";
            redisTemplate.delete(redisTemplate.keys(pattern));
            
            log.debug("Evicted cache for context: {}", contextId);
        } catch (Exception e) {
            log.error("Error evicting context cache", e);
        }
    }
    
    /**
     * Cache context metadata.
     */
    public void cacheContextMetadata(UUID contextId, String metadata) {
        try {
            String key = CONTEXT_CACHE_PREFIX + contextId + ":metadata";
            redisTemplate.opsForValue().set(key, metadata, cacheTtlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error caching context metadata", e);
        }
    }
    
    /**
     * Get cached context metadata.
     */
    public String getContextMetadata(UUID contextId) {
        try {
            String key = CONTEXT_CACHE_PREFIX + contextId + ":metadata";
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Error retrieving context metadata from cache", e);
            return null;
        }
    }
    
    /**
     * Build cache key for context window.
     */
    private String buildWindowCacheKey(UUID contextId, ContextWindowRequest request) {
        return WINDOW_CACHE_PREFIX + contextId + ":" + 
               request.getMaxTokens() + ":" + 
               request.getMessageLimit() + ":" +
               request.getIncludeSystemMessages() + ":" +
               request.getPreserveMessageBoundaries();
    }
}