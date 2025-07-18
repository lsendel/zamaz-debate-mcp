package com.zamaz.mcp.llm.application.port.outbound;

import com.zamaz.mcp.llm.domain.model.*;
import java.time.Duration;
import java.util.Optional;

/**
 * Cache service for storing completion responses to avoid redundant API calls.
 * This is an outbound port in hexagonal architecture.
 */
public interface CompletionCacheService {
    
    /**
     * Cache a completion response with a TTL.
     */
    void cacheCompletion(
        String cacheKey,
        LlmProviderGateway.CompletionResponse response,
        Duration ttl
    );
    
    /**
     * Retrieve a cached completion response.
     */
    Optional<LlmProviderGateway.CompletionResponse> getCachedCompletion(String cacheKey);
    
    /**
     * Generate a cache key for a completion request.
     */
    String generateCacheKey(CompletionRequest request, ModelName model, ProviderId providerId);
    
    /**
     * Invalidate cached responses for a specific provider.
     */
    void invalidateProviderCache(ProviderId providerId);
    
    /**
     * Invalidate cached responses for a specific model.
     */
    void invalidateModelCache(ProviderId providerId, ModelName model);
    
    /**
     * Clear all cached completions.
     */
    void clearAll();
    
    /**
     * Get cache statistics.
     */
    CacheStats getStats();
    
    /**
     * Cache statistics.
     */
    record CacheStats(
        long hitCount,
        long missCount,
        long evictionCount,
        double hitRate,
        long estimatedSize
    ) {}
}