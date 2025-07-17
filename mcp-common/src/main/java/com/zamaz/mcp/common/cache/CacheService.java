package com.zamaz.mcp.common.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Generic cache service interface for MCP services.
 * 
 * @param <K> The type of the cache key
 * @param <V> The type of the cached value
 */
public interface CacheService<K, V> {

    /**
     * Get a value from the cache.
     *
     * @param key The cache key
     * @return The cached value, or empty if not found
     */
    Optional<V> get(K key);

    /**
     * Put a value in the cache.
     *
     * @param key The cache key
     * @param value The value to cache
     */
    void put(K key, V value);

    /**
     * Put a value in the cache with a specific time-to-live.
     *
     * @param key The cache key
     * @param value The value to cache
     * @param ttl The time-to-live for the cached value
     */
    void put(K key, V value, Duration ttl);

    /**
     * Remove a value from the cache.
     *
     * @param key The cache key
     * @return true if the value was removed, false otherwise
     */
    boolean remove(K key);

    /**
     * Check if a key exists in the cache.
     *
     * @param key The cache key
     * @return true if the key exists, false otherwise
     */
    boolean exists(K key);

    /**
     * Clear all entries from the cache.
     */
    void clear();
    
    /**
     * Get a value from the cache, or compute it if not present.
     *
     * @param key The cache key
     * @param supplier A function to compute the value if not in cache
     * @return The cached or computed value
     */
    V getOrCompute(K key, CacheValueSupplier<V> supplier);
    
    /**
     * Get a value from the cache, or compute it if not present, with a specific TTL.
     *
     * @param key The cache key
     * @param supplier A function to compute the value if not in cache
     * @param ttl The time-to-live for the cached value
     * @return The cached or computed value
     */
    V getOrCompute(K key, CacheValueSupplier<V> supplier, Duration ttl);
    
    /**
     * Functional interface for computing cache values.
     *
     * @param <V> The type of the value
     */
    @FunctionalInterface
    interface CacheValueSupplier<V> {
        V get();
    }
}
