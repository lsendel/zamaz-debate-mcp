package com.zamaz.mcp.sidecar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced Caching Service for MCP Sidecar
 * 
 * Provides intelligent caching strategies for:
 * - API responses
 * - Authentication tokens
 * - User sessions
 * - AI/LLM responses
 * - Organization data
 * - Rate limiting counters
 * - Circuit breaker states
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CachingService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${app.cache.default-ttl:300}")
    private Duration defaultTtl;

    @Value("${app.cache.ai-response-ttl:1800}")
    private Duration aiResponseTtl;

    @Value("${app.cache.auth-token-ttl:3600}")
    private Duration authTokenTtl;

    @Value("${app.cache.user-session-ttl:86400}")
    private Duration userSessionTtl;

    @Value("${app.cache.organization-data-ttl:3600}")
    private Duration organizationDataTtl;

    @Value("${app.cache.enabled:true}")
    private boolean cachingEnabled;

    // Cache categories
    public enum CacheCategory {
        API_RESPONSE("api:response"),
        AI_RESPONSE("ai:response"),
        AUTH_TOKEN("auth:token"),
        USER_SESSION("user:session"),
        ORGANIZATION_DATA("org:data"),
        RATE_LIMIT("rate:limit"),
        CIRCUIT_BREAKER("circuit:breaker"),
        HEALTH_CHECK("health:check"),
        PERMISSION_CACHE("permission:cache");

        private final String prefix;

        CacheCategory(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    // Cache statistics
    private final Map<CacheCategory, CacheStats> cacheStats = new ConcurrentHashMap<>();

    /**
     * Cache statistics
     */
    public static class CacheStats {
        private long hits = 0;
        private long misses = 0;
        private long evictions = 0;
        private long errors = 0;

        public synchronized void recordHit() { hits++; }
        public synchronized void recordMiss() { misses++; }
        public synchronized void recordEviction() { evictions++; }
        public synchronized void recordError() { errors++; }

        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getEvictions() { return evictions; }
        public long getErrors() { return errors; }
        public double getHitRate() { 
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }
    }

    /**
     * Cached item with metadata
     */
    public static class CachedItem {
        private final String value;
        private final Instant createdAt;
        private final Instant expiresAt;
        private final Map<String, String> metadata;

        public CachedItem(String value, Duration ttl) {
            this.value = value;
            this.createdAt = Instant.now();
            this.expiresAt = createdAt.plus(ttl);
            this.metadata = new HashMap<>();
        }

        public String getValue() { return value; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getExpiresAt() { return expiresAt; }
        public Map<String, String> getMetadata() { return metadata; }
        public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
        public Duration getTimeToLive() { 
            return Duration.between(Instant.now(), expiresAt);
        }
    }

    /**
     * Get cached item
     */
    public Mono<String> get(CacheCategory category, String key) {
        if (!cachingEnabled) {
            return Mono.empty();
        }

        String cacheKey = buildCacheKey(category, key);
        
        return redisTemplate.opsForValue()
                .get(cacheKey)
                .doOnNext(value -> {
                    if (value != null) {
                        recordCacheHit(category);
                        log.debug("Cache hit: category={}, key={}", category, key);
                    } else {
                        recordCacheMiss(category);
                        log.debug("Cache miss: category={}, key={}", category, key);
                    }
                })
                .onErrorResume(error -> {
                    recordCacheError(category);
                    log.error("Cache get error: category={}, key={}, error={}", 
                            category, key, error.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Set cached item with default TTL
     */
    public Mono<Void> set(CacheCategory category, String key, String value) {
        return set(category, key, value, getTtlForCategory(category));
    }

    /**
     * Set cached item with custom TTL
     */
    public Mono<Void> set(CacheCategory category, String key, String value, Duration ttl) {
        if (!cachingEnabled) {
            return Mono.empty();
        }

        String cacheKey = buildCacheKey(category, key);
        
        return redisTemplate.opsForValue()
                .set(cacheKey, value, ttl)
                .doOnSuccess(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        log.debug("Cache set: category={}, key={}, ttl={}", category, key, ttl);
                    } else {
                        log.warn("Cache set failed: category={}, key={}", category, key);
                    }
                })
                .onErrorResume(error -> {
                    recordCacheError(category);
                    log.error("Cache set error: category={}, key={}, error={}", 
                            category, key, error.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Delete cached item
     */
    public Mono<Void> delete(CacheCategory category, String key) {
        if (!cachingEnabled) {
            return Mono.empty();
        }

        String cacheKey = buildCacheKey(category, key);
        
        return redisTemplate.delete(cacheKey)
                .doOnNext(deleted -> {
                    if (deleted > 0) {
                        log.debug("Cache delete: category={}, key={}", category, key);
                    }
                })
                .onErrorResume(error -> {
                    recordCacheError(category);
                    log.error("Cache delete error: category={}, key={}, error={}", 
                            category, key, error.getMessage());
                    return Mono.just(0L);
                })
                .then();
    }

    /**
     * Check if key exists in cache
     */
    public Mono<Boolean> exists(CacheCategory category, String key) {
        if (!cachingEnabled) {
            return Mono.just(false);
        }

        String cacheKey = buildCacheKey(category, key);
        
        return redisTemplate.hasKey(cacheKey)
                .onErrorReturn(false);
    }

    /**
     * Get cached item with fallback
     */
    public Mono<String> getOrSet(CacheCategory category, String key, 
                                 Mono<String> fallback, Duration ttl) {
        return get(category, key)
                .switchIfEmpty(fallback
                        .flatMap(value -> set(category, key, value, ttl)
                                .then(Mono.just(value))));
    }

    /**
     * Get cached item with fallback using default TTL
     */
    public Mono<String> getOrSet(CacheCategory category, String key, Mono<String> fallback) {
        return getOrSet(category, key, fallback, getTtlForCategory(category));
    }

    /**
     * Cache AI response with intelligent key generation
     */
    public Mono<String> cacheAIResponse(String prompt, String model, Map<String, Object> parameters, 
                                      String response) {
        String cacheKey = generateAIResponseKey(prompt, model, parameters);
        
        return set(CacheCategory.AI_RESPONSE, cacheKey, response, aiResponseTtl)
                .then(Mono.just(cacheKey));
    }

    /**
     * Get cached AI response
     */
    public Mono<String> getCachedAIResponse(String prompt, String model, Map<String, Object> parameters) {
        String cacheKey = generateAIResponseKey(prompt, model, parameters);
        return get(CacheCategory.AI_RESPONSE, cacheKey);
    }

    /**
     * Cache user session
     */
    public Mono<Void> cacheUserSession(String userId, String sessionData) {
        return set(CacheCategory.USER_SESSION, userId, sessionData, userSessionTtl);
    }

    /**
     * Get cached user session
     */
    public Mono<String> getCachedUserSession(String userId) {
        return get(CacheCategory.USER_SESSION, userId);
    }

    /**
     * Cache organization data
     */
    public Mono<Void> cacheOrganizationData(String organizationId, String data) {
        return set(CacheCategory.ORGANIZATION_DATA, organizationId, data, organizationDataTtl);
    }

    /**
     * Get cached organization data
     */
    public Mono<String> getCachedOrganizationData(String organizationId) {
        return get(CacheCategory.ORGANIZATION_DATA, organizationId);
    }

    /**
     * Cache permission data
     */
    public Mono<Void> cachePermissionData(String userId, String organizationId, String permissions) {
        String key = userId + ":" + organizationId;
        return set(CacheCategory.PERMISSION_CACHE, key, permissions, Duration.ofMinutes(30));
    }

    /**
     * Get cached permission data
     */
    public Mono<String> getCachedPermissionData(String userId, String organizationId) {
        String key = userId + ":" + organizationId;
        return get(CacheCategory.PERMISSION_CACHE, key);
    }

    /**
     * Invalidate cache by pattern
     */
    public Mono<Void> invalidateByPattern(CacheCategory category, String pattern) {
        String searchPattern = buildCacheKey(category, pattern);
        
        return redisTemplate.keys(searchPattern)
                .flatMap(redisTemplate::delete)
                .doOnNext(deleted -> {
                    if (deleted > 0) {
                        log.debug("Cache invalidation: category={}, pattern={}, deleted={}", 
                                category, pattern, deleted);
                    }
                })
                .then();
    }

    /**
     * Clear all cache for category
     */
    public Mono<Void> clearCategory(CacheCategory category) {
        return invalidateByPattern(category, "*");
    }

    /**
     * Get cache statistics
     */
    public Map<CacheCategory, CacheStats> getCacheStatistics() {
        return new HashMap<>(cacheStats);
    }

    /**
     * Get cache info
     */
    public Mono<Map<String, Object>> getCacheInfo() {
        return redisTemplate.getConnectionFactory().getReactiveConnection()
                .serverCommands()
                .info("memory")
                .map(info -> {
                    Map<String, Object> cacheInfo = new HashMap<>();
                    cacheInfo.put("enabled", cachingEnabled);
                    cacheInfo.put("defaultTtl", defaultTtl.toString());
                    cacheInfo.put("categories", cacheStats.keySet());
                    cacheInfo.put("statistics", getCacheStatistics());
                    cacheInfo.put("redisMemoryInfo", info);
                    return cacheInfo;
                });
    }

    /**
     * Warm up cache with frequently used data
     */
    public Mono<Void> warmUpCache() {
        return Mono.fromRunnable(() -> {
            log.info("Starting cache warm-up process");
            
            // This could be extended to pre-load frequently accessed data
            // For now, just log the warm-up initiation
            
            log.info("Cache warm-up process completed");
        });
    }

    /**
     * Build cache key
     */
    private String buildCacheKey(CacheCategory category, String key) {
        return category.getPrefix() + ":" + key;
    }

    /**
     * Generate AI response cache key
     */
    private String generateAIResponseKey(String prompt, String model, Map<String, Object> parameters) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(model).append(":");
        keyBuilder.append(hashString(prompt)).append(":");
        
        if (parameters != null) {
            parameters.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> keyBuilder.append(entry.getKey())
                            .append("=")
                            .append(entry.getValue())
                            .append(":"));
        }
        
        return hashString(keyBuilder.toString());
    }

    /**
     * Hash string for cache key
     */
    private String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error hashing string for cache key", e);
            return Integer.toString(input.hashCode());
        }
    }

    /**
     * Get TTL for cache category
     */
    private Duration getTtlForCategory(CacheCategory category) {
        return switch (category) {
            case AI_RESPONSE -> aiResponseTtl;
            case AUTH_TOKEN -> authTokenTtl;
            case USER_SESSION -> userSessionTtl;
            case ORGANIZATION_DATA -> organizationDataTtl;
            case RATE_LIMIT -> Duration.ofMinutes(15);
            case CIRCUIT_BREAKER -> Duration.ofMinutes(5);
            case HEALTH_CHECK -> Duration.ofMinutes(2);
            case PERMISSION_CACHE -> Duration.ofMinutes(30);
            default -> defaultTtl;
        };
    }

    /**
     * Record cache hit
     */
    private void recordCacheHit(CacheCategory category) {
        cacheStats.computeIfAbsent(category, k -> new CacheStats()).recordHit();
    }

    /**
     * Record cache miss
     */
    private void recordCacheMiss(CacheCategory category) {
        cacheStats.computeIfAbsent(category, k -> new CacheStats()).recordMiss();
    }

    /**
     * Record cache error
     */
    private void recordCacheError(CacheCategory category) {
        cacheStats.computeIfAbsent(category, k -> new CacheStats()).recordError();
    }
}