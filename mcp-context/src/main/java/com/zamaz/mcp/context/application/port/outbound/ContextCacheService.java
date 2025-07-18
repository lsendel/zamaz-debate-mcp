package com.zamaz.mcp.context.application.port.outbound;

import com.zamaz.mcp.context.domain.model.ContextId;
import com.zamaz.mcp.context.domain.model.ContextMetadata;
import com.zamaz.mcp.context.domain.model.ContextWindow;
import java.time.Duration;
import java.util.Optional;

/**
 * Outbound port for caching context-related data.
 * Implementations may use different caching strategies (Redis, in-memory, etc.).
 */
public interface ContextCacheService {
    
    /**
     * Cache a context window with a specific key.
     * 
     * @param key The cache key
     * @param window The context window to cache
     * @param ttl Time to live for the cache entry
     */
    void cacheWindow(String key, ContextWindow window, Duration ttl);
    
    /**
     * Retrieve a cached context window.
     * 
     * @param key The cache key
     * @return The cached window if present
     */
    Optional<ContextWindow> getCachedWindow(String key);
    
    /**
     * Cache context metadata.
     * 
     * @param contextId The context ID
     * @param metadata The metadata to cache
     */
    void cacheMetadata(ContextId contextId, ContextMetadata metadata);
    
    /**
     * Retrieve cached context metadata.
     * 
     * @param contextId The context ID
     * @return The cached metadata if present
     */
    Optional<ContextMetadata> getCachedMetadata(ContextId contextId);
    
    /**
     * Evict all cache entries for a specific context.
     * 
     * @param contextId The context ID
     */
    void evictContext(ContextId contextId);
    
    /**
     * Evict all cache entries for a specific organization.
     * 
     * @param organizationId The organization ID
     */
    void evictOrganization(String organizationId);
    
    /**
     * Clear all cached data.
     */
    void clearAll();
    
    /**
     * Generate a cache key for context windows.
     * 
     * @param contextId The context ID
     * @param maxTokens Maximum tokens in window
     * @param maxMessages Maximum messages in window
     * @return The cache key
     */
    String generateWindowCacheKey(ContextId contextId, int maxTokens, int maxMessages);
}