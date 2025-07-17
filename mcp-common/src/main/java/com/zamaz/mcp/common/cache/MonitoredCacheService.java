package com.zamaz.mcp.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

/**
 * A cache service implementation that monitors cache operations and collects metrics.
 *
 * @param <K> The type of the cache key
 * @param <V> The type of the cached value
 */
@Slf4j
@RequiredArgsConstructor
public class MonitoredCacheService<K, V> implements CacheService<K, V> {

    private final CacheService<K, V> delegate;
    private final CacheMetricsCollector metricsCollector;
    private final String cacheName;

    /**
     * Get a value from the cache.
     *
     * @param key The cache key
     * @return The cached value, or empty if not found
     */
    @Override
    public Optional<V> get(K key) {
        long startTime = System.nanoTime();
        try {
            Optional<V> result = delegate.get(key);
            if (result.isPresent()) {
                metricsCollector.recordHit(cacheName);
            } else {
                metricsCollector.recordMiss(cacheName);
            }
            return result;
        } finally {
            metricsCollector.recordGetTime(cacheName, System.nanoTime() - startTime);
        }
    }

    /**
     * Put a value in the cache.
     *
     * @param key The cache key
     * @param value The value to cache
     */
    @Override
    public void put(K key, V value) {
        long startTime = System.nanoTime();
        try {
            delegate.put(key, value);
        } finally {
            metricsCollector.recordPutTime(cacheName, System.nanoTime() - startTime);
        }
    }

    /**
     * Put a value in the cache with a specific time-to-live.
     *
     * @param key The cache key
     * @param value The value to cache
     * @param ttl The time-to-live for the cached value
     */
    @Override
    public void put(K key, V value, Duration ttl) {
        long startTime = System.nanoTime();
        try {
            delegate.put(key, value, ttl);
        } finally {
            metricsCollector.recordPutTime(cacheName, System.nanoTime() - startTime);
        }
    }

    /**
     * Remove a value from the cache.
     *
     * @param key The cache key
     * @return true if the value was removed, false otherwise
     */
    @Override
    public boolean remove(K key) {
        return delegate.remove(key);
    }

    /**
     * Check if a key exists in the cache.
     *
     * @param key The cache key
     * @return true if the key exists, false otherwise
     */
    @Override
    public boolean exists(K key) {
        return delegate.exists(key);
    }

    /**
     * Clear all entries from the cache.
     */
    @Override
    public void clear() {
        delegate.clear();
    }

    /**
     * Get a value from the cache, or compute it if not present.
     *
     * @param key The cache key
     * @param supplier A function to compute the value if not in cache
     * @return The cached or computed value
     */
    @Override
    public V getOrCompute(K key, CacheValueSupplier<V> supplier) {
        long startTime = System.nanoTime();
        Optional<V> cachedValue = get(key);
        
        if (cachedValue.isPresent()) {
            return cachedValue.get();
        }
        
        V computedValue = supplier.get();
        put(key, computedValue);
        return computedValue;
    }

    /**
     * Get a value from the cache, or compute it if not present, with a specific TTL.
     *
     * @param key The cache key
     * @param supplier A function to compute the value if not in cache
     * @param ttl The time-to-live for the cached value
     * @return The cached or computed value
     */
    @Override
    public V getOrCompute(K key, CacheValueSupplier<V> supplier, Duration ttl) {
        long startTime = System.nanoTime();
        Optional<V> cachedValue = get(key);
        
        if (cachedValue.isPresent()) {
            return cachedValue.get();
        }
        
        V computedValue = supplier.get();
        put(key, computedValue, ttl);
        return computedValue;
    }
}
